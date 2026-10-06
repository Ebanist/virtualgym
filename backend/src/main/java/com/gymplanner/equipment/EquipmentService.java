package com.gymplanner.equipment;

import com.gymplanner.common.error.BusinessRuleException;
import com.gymplanner.common.error.ConflictException;
import com.gymplanner.common.error.NotFoundException;
import com.gymplanner.common.text.TextNormalizer;
import com.gymplanner.common.web.PageResponse;
import com.gymplanner.equipment.dto.CreateEquipmentRequest;
import com.gymplanner.equipment.dto.EquipmentChangeDto;
import com.gymplanner.equipment.dto.EquipmentDto;
import com.gymplanner.equipment.dto.EquipmentSummaryDto;
import com.gymplanner.equipment.dto.EquipmentTypeDto;
import com.gymplanner.equipment.dto.UpdateEquipmentRequest;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymAccessService;
import com.gymplanner.storage.ImageStorageService;
import com.gymplanner.storage.StoredFile;
import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EquipmentService {

    static final double SIMILARITY_THRESHOLD = 0.3;
    private static final int SIMILAR_LIMIT = 5;

    private final EquipmentRepository equipment;
    private final EquipmentTypeRepository types;
    private final EquipmentChangeRepository changes;
    private final EquipmentReportRepository reports;
    private final GymAccessService access;
    private final UserRepository users;
    private final ImageStorageService images;
    private final Clock clock;

    public EquipmentService(EquipmentRepository equipment, EquipmentTypeRepository types,
            EquipmentChangeRepository changes, EquipmentReportRepository reports, GymAccessService access,
            UserRepository users, ImageStorageService images, Clock clock) {
        this.equipment = equipment;
        this.types = types;
        this.changes = changes;
        this.reports = reports;
        this.access = access;
        this.users = users;
        this.images = images;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<EquipmentTypeDto> listTypes() {
        return types.findAllByOrderByCategoryAscNameAsc().stream().map(EquipmentTypeDto::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<EquipmentSummaryDto> search(UUID gymId, String q, EquipmentCategory category,
            EquipmentStatus status, Pageable pageable) {
        access.getGym(gymId);
        String name = Objects.requireNonNullElse(TextNormalizer.normalize(q), "");
        Page<Equipment> page = equipment.search(gymId, name, category, status, pageable);
        Map<UUID, Long> openReports = openReportCounts(page.getContent());
        return PageResponse.of(page, e -> EquipmentMapper.toSummary(e, openReports.getOrDefault(e.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public List<EquipmentSummaryDto> findSimilar(UUID gymId, String name) {
        String normalized = TextNormalizer.normalize(name);
        if (normalized == null || normalized.length() < 3) {
            return List.of();
        }
        List<Equipment> similar = equipment.findSimilar(gymId, normalized, SIMILARITY_THRESHOLD, SIMILAR_LIMIT);
        Map<UUID, Long> openReports = openReportCounts(similar);
        return similar.stream()
                .map(e -> EquipmentMapper.toSummary(e, openReports.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipmentDto get(UUID userId, UUID id) {
        Equipment e = getActive(id);
        return toDto(e, access.isMember(userId, e.getGym().getId()));
    }

    @Transactional
    public EquipmentDto create(UUID userId, UUID gymId, CreateEquipmentRequest request) {
        Gym gym = access.getGym(gymId);
        access.requireMember(userId, gymId);
        User user = users.getReferenceById(userId);
        Equipment e = new Equipment(gym, request.name().trim(), request.category(), user);
        e.setEquipmentType(resolveType(request.equipmentTypeId()));
        e.setDescription(blankToNull(request.description()));
        e.setQuantity(request.quantity());
        equipment.save(e);
        changes.save(new EquipmentChange(e, user, ChangeType.CREATED, diff(Snapshot.empty(), Snapshot.of(e))));
        return toDto(e, true);
    }

    @Transactional
    public EquipmentDto update(UUID userId, UUID id, UpdateEquipmentRequest request) {
        Equipment e = getActive(id);
        access.requireMember(userId, e.getGym().getId());
        if (request.version() != e.getVersion()) {
            throw new ConflictException("concurrent_modification",
                    "The equipment was modified by someone else. Reload and try again.");
        }
        Snapshot before = Snapshot.of(e);
        e.setName(request.name().trim());
        e.setCategory(request.category());
        e.setEquipmentType(resolveType(request.equipmentTypeId()));
        e.setDescription(blankToNull(request.description()));
        e.setQuantity(request.quantity());
        e.setStatus(request.status());
        Map<String, FieldChange> diff = diff(before, Snapshot.of(e));
        if (!diff.isEmpty()) {
            ChangeType type = diff.keySet().equals(Set.of("status")) ? ChangeType.STATUS_CHANGED : ChangeType.UPDATED;
            changes.save(new EquipmentChange(e, users.getReferenceById(userId), type, diff));
            equipment.flush(); // podbija wersję przed zbudowaniem odpowiedzi
        }
        return toDto(e, true);
    }

    @Transactional
    public EquipmentDto uploadPhoto(UUID userId, UUID id, MultipartFile file) {
        Equipment e = getActive(id);
        access.requireMember(userId, e.getGym().getId());
        User user = users.getReferenceById(userId);
        StoredFile photo = images.storeImage(file, user);
        String oldPhoto = e.getPhoto() == null ? null : e.getPhoto().getId().toString();
        e.setPhoto(photo);
        changes.save(new EquipmentChange(e, user, ChangeType.PHOTO_CHANGED,
                Map.of("photo", new FieldChange(oldPhoto, photo.getId().toString()))));
        equipment.flush();
        return toDto(e, true);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Equipment e = getActive(id);
        access.requireMember(userId, e.getGym().getId());
        e.markDeleted(clock.instant());
        changes.save(new EquipmentChange(e, users.getReferenceById(userId), ChangeType.DELETED, Map.of()));
    }

    @Transactional(readOnly = true)
    public PageResponse<EquipmentChangeDto> history(UUID id, int page, int size) {
        getActive(id);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(changes.findByEquipmentId(id, pageable), EquipmentMapper::toDto);
    }

    /** Sprzęt (nieusunięty) – dla innych modułów (ćwiczenia, plany). */
    @Transactional(readOnly = true)
    public Equipment getActive(UUID id) {
        return equipment.findActiveById(id).orElseThrow(() -> new NotFoundException("Equipment"));
    }

    private EquipmentDto toDto(Equipment e, boolean member) {
        long open = reports.countByEquipmentIdAndStatus(e.getId(), ReportStatus.OPEN);
        return EquipmentMapper.toDto(e, open, member);
    }

    private Map<UUID, Long> openReportCounts(List<Equipment> list) {
        if (list.isEmpty()) {
            return Map.of();
        }
        Set<UUID> ids = list.stream().map(Equipment::getId).collect(Collectors.toSet());
        return reports.countOpenByEquipmentIds(ids).stream()
                .collect(Collectors.toMap(EquipmentReportRepository.OpenReportCount::getEquipmentId,
                        EquipmentReportRepository.OpenReportCount::getCount));
    }

    private EquipmentType resolveType(UUID typeId) {
        if (typeId == null) {
            return null;
        }
        return types.findById(typeId)
                .orElseThrow(() -> new BusinessRuleException("equipment_type_not_found", "Unknown equipment type"));
    }

    private static Map<String, FieldChange> diff(Snapshot before, Snapshot after) {
        Map<String, FieldChange> result = new LinkedHashMap<>();
        put(result, "name", before.name(), after.name());
        put(result, "category", before.category(), after.category());
        put(result, "equipmentType", before.equipmentType(), after.equipmentType());
        put(result, "description", before.description(), after.description());
        put(result, "quantity", before.quantity(), after.quantity());
        put(result, "status", before.status(), after.status());
        return result;
    }

    private static void put(Map<String, FieldChange> result, String field, String oldValue, String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            result.put(field, new FieldChange(oldValue, newValue));
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Wartości pól w postaci tekstowej – do porównania i zapisu w historii. */
    private record Snapshot(String name, String category, String equipmentType, String description,
            String quantity, String status) {

        static Snapshot empty() {
            return new Snapshot(null, null, null, null, null, null);
        }

        static Snapshot of(Equipment e) {
            return new Snapshot(e.getName(), e.getCategory().name(),
                    e.getEquipmentType() == null ? null : e.getEquipmentType().getName(), e.getDescription(),
                    e.getQuantity() == null ? null : e.getQuantity().toString(), e.getStatus().name());
        }
    }
}
