package com.gymplanner.exercise;

import com.gymplanner.common.error.BusinessRuleException;
import com.gymplanner.common.error.ConflictException;
import com.gymplanner.common.error.ForbiddenException;
import com.gymplanner.common.error.NotFoundException;
import com.gymplanner.common.text.TextNormalizer;
import com.gymplanner.common.text.TextSimilarity;
import com.gymplanner.equipment.Equipment;
import com.gymplanner.equipment.EquipmentRepository;
import com.gymplanner.equipment.EquipmentService;
import com.gymplanner.exercise.ExerciseAvailabilityResolver.Availability;
import com.gymplanner.exercise.ExerciseAvailabilityResolver.ExerciseRequirement;
import com.gymplanner.exercise.ExerciseAvailabilityResolver.GymEquipment;
import com.gymplanner.exercise.dto.AvailableExerciseDto;
import com.gymplanner.exercise.dto.CreateExerciseRequest;
import com.gymplanner.exercise.dto.EquipmentExerciseDto;
import com.gymplanner.exercise.dto.EquipmentOptionDto;
import com.gymplanner.exercise.dto.ExerciseDto;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymAccessService;
import com.gymplanner.gym.GymMembership;
import com.gymplanner.gym.GymMembershipRepository;
import com.gymplanner.storage.FileController;
import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import java.time.Clock;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ćwiczenia: biblioteka (GLOBAL) i własne (CUSTOM, prywatne lub publiczne w siłowni).
 * Wszystkie listy i walidacje respektują widoczność – cudze prywatne ćwiczenia „nie istnieją” (404).
 */
@Service
public class ExerciseService {

    static final double SIMILARITY_THRESHOLD = 0.7;
    private static final int SIMILAR_LIMIT = 5;

    private final ExerciseRepository exercises;
    private final EquipmentExerciseLinkRepository links;
    private final EquipmentRepository equipment;
    private final EquipmentService equipmentService;
    private final GymAccessService access;
    private final GymMembershipRepository memberships;
    private final UserRepository users;
    private final Clock clock;

    public ExerciseService(ExerciseRepository exercises, EquipmentExerciseLinkRepository links,
            EquipmentRepository equipment, EquipmentService equipmentService, GymAccessService access,
            GymMembershipRepository memberships, UserRepository users, Clock clock) {
        this.exercises = exercises;
        this.links = links;
        this.equipment = equipment;
        this.equipmentService = equipmentService;
        this.access = access;
        this.memberships = memberships;
        this.users = users;
        this.clock = clock;
    }

    /**
     * Biblioteka: ćwiczenia globalne + widoczne własne ćwiczenia siłowni użytkownika
     * (albo tylko wskazanej siłowni, gdy podano {@code gymId}).
     */
    @Transactional(readOnly = true)
    public List<ExerciseDto> library(UUID userId, UUID gymId, String q, MuscleGroup muscle) {
        Set<UUID> gymIds;
        if (gymId != null) {
            access.getGym(gymId);
            gymIds = Set.of(gymId);
        } else {
            gymIds = memberships.findByUserIdWithGym(userId).stream()
                    .map(GymMembership::getGym).map(Gym::getId).collect(Collectors.toSet());
        }
        return filter(exercises.findVisible(userId, withPlaceholder(gymIds)).stream(), q, muscle)
                .map(e -> ExerciseMapper.toDto(e, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExerciseDto get(UUID userId, UUID id) {
        Exercise e = getVisible(userId, id);
        return ExerciseMapper.toDto(e, userId, e.isAuthor(userId) ? linkedEquipmentIds(e) : null);
    }

    /** Ćwiczenia możliwe do wykonania w siłowni (+ z masą ciała) wraz ze sprzętem. */
    @Transactional(readOnly = true)
    public List<AvailableExerciseDto> available(UUID userId, UUID gymId, String q, MuscleGroup muscle) {
        GymExerciseCatalog catalog = catalog(gymId, userId);
        return filter(catalog.exercises().stream(), q, muscle)
                .filter(e -> catalog.availability().containsKey(e.getId()))
                .map(e -> new AvailableExerciseDto(ExerciseMapper.toDto(e, userId),
                        catalog.availability().get(e.getId()).equipmentIds().stream()
                                .map(catalog.equipmentById()::get)
                                .map(ExerciseService::toOption)
                                .toList()))
                .toList();
    }

    /** Ćwiczenia widoczne dla użytkownika o nazwie podobnej do podanej (ograniczanie duplikatów). */
    @Transactional(readOnly = true)
    public List<ExerciseDto> similar(UUID userId, UUID gymId, String name) {
        access.getGym(gymId);
        String needle = TextNormalizer.normalize(name);
        if (needle == null || needle.length() < 3) {
            return List.of();
        }
        return exercises.findVisible(userId, Set.of(gymId)).stream()
                .filter(e -> TextSimilarity.similar(e.getNormalizedName(), needle, SIMILARITY_THRESHOLD))
                .sorted((a, b) -> Double.compare(TextSimilarity.dice(b.getNormalizedName(), needle),
                        TextSimilarity.dice(a.getNormalizedName(), needle)))
                .limit(SIMILAR_LIMIT)
                .map(e -> ExerciseMapper.toDto(e, userId))
                .toList();
    }

    /** Migawka dostępności ćwiczeń w siłowni dla użytkownika – również do walidacji planów i treningów. */
    @Transactional(readOnly = true)
    public GymExerciseCatalog catalog(UUID gymId, UUID userId) {
        access.getGym(gymId);
        List<Exercise> visible = exercises.findVisible(userId, Set.of(gymId));
        List<Equipment> gymEquipment = equipment.findAllActiveInGym(gymId);
        Map<UUID, Set<UUID>> linkMap = new HashMap<>();
        for (EquipmentExerciseLink link : links.findByGym(gymId)) {
            linkMap.computeIfAbsent(link.getExercise().getId(), k -> new HashSet<>()).add(link.getEquipment().getId());
        }
        Map<UUID, Availability> availability = ExerciseAvailabilityResolver.resolve(
                visible.stream().map(ExerciseService::requirement).toList(),
                gymEquipment.stream().map(ExerciseService::gymEquipment).toList(),
                linkMap);
        Map<UUID, Equipment> byId = gymEquipment.stream()
                .collect(Collectors.toMap(Equipment::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        return new GymExerciseCatalog(visible, byId, availability);
    }

    @Transactional
    public ExerciseDto createCustom(UUID userId, UUID gymId, CreateExerciseRequest request) {
        Gym gym = access.getGym(gymId);
        access.requireMember(userId, gymId);
        List<Equipment> selected = validatedEquipment(gymId, request);
        User user = users.getReferenceById(userId);
        Exercise exercise = exercises.save(Exercise.custom(gym, user, details(request)));
        selected.forEach(e -> links.save(new EquipmentExerciseLink(e, exercise, user)));
        return ExerciseMapper.toDto(exercise, userId, selected.stream().map(Equipment::getId).toList());
    }

    /**
     * Edycja własnego ćwiczenia (tylko autor). Sprzęt z żądania zastępuje powiązania ćwiczenia.
     * Publicznego ćwiczenia używanego w planach innych osób nie można uczynić prywatnym.
     */
    @Transactional
    public ExerciseDto update(UUID userId, UUID id, CreateExerciseRequest request) {
        Exercise exercise = getOwnCustom(userId, id);
        UUID gymId = exercise.getGym().getId();
        ExerciseVisibility newVisibility = visibility(request);
        if (exercise.getVisibility() == ExerciseVisibility.GYM && newVisibility == ExerciseVisibility.PRIVATE
                && exercises.isUsedInPlansOfOthers(id, userId)) {
            throw new ConflictException("exercise_used_by_others",
                    "The exercise is used in other members' plans and cannot be made private");
        }
        List<Equipment> selected = validatedEquipment(gymId, request);
        exercise.apply(details(request));
        Set<UUID> wanted = selected.stream().map(Equipment::getId).collect(Collectors.toSet());
        List<EquipmentExerciseLink> current = links.findByExerciseId(id);
        current.stream().filter(l -> !wanted.contains(l.getEquipment().getId())).forEach(links::delete);
        Set<UUID> existing = current.stream().map(l -> l.getEquipment().getId()).collect(Collectors.toSet());
        User user = users.getReferenceById(userId);
        selected.stream().filter(e -> !existing.contains(e.getId()))
                .forEach(e -> links.save(new EquipmentExerciseLink(e, exercise, user)));
        return ExerciseMapper.toDto(exercise, userId, List.copyOf(wanted));
    }

    /** Usunięcie własnego ćwiczenia (tylko autor) – ukrywa je z list; plany i historia zostają. */
    @Transactional
    public void delete(UUID userId, UUID id) {
        getOwnCustom(userId, id).markDeleted(clock.instant());
    }

    /** Ćwiczenia wykonywane na danym sprzęcie: dopasowane przez typ lub jawnie powiązane. */
    @Transactional(readOnly = true)
    public List<EquipmentExerciseDto> forEquipment(UUID userId, UUID equipmentId) {
        Equipment item = equipmentService.getActive(equipmentId);
        UUID typeId = item.getEquipmentType() == null ? null : item.getEquipmentType().getId();
        Set<UUID> linked = links.findByGym(item.getGym().getId()).stream()
                .filter(l -> l.getEquipment().getId().equals(equipmentId))
                .map(l -> l.getExercise().getId())
                .collect(Collectors.toSet());
        return exercises.findVisible(userId, Set.of(item.getGym().getId())).stream()
                .map(e -> {
                    boolean byType = typeId != null && e.getEquipmentTypes().stream()
                            .anyMatch(t -> t.getId().equals(typeId));
                    return new EquipmentExerciseDto(ExerciseMapper.toDto(e, userId), linked.contains(e.getId()),
                            byType);
                })
                .filter(dto -> dto.linked() || dto.byType())
                .toList();
    }

    @Transactional
    public void link(UUID userId, UUID equipmentId, UUID exerciseId) {
        Equipment item = equipmentService.getActive(equipmentId);
        access.requireMember(userId, item.getGym().getId());
        Exercise exercise = exercises.findById(exerciseId)
                .filter(e -> e.isVisibleTo(userId, item.getGym().getId()))
                .orElseThrow(() -> new NotFoundException("Exercise"));
        if (!links.existsByEquipmentIdAndExerciseId(equipmentId, exerciseId)) {
            links.save(new EquipmentExerciseLink(item, exercise, users.getReferenceById(userId)));
        }
    }

    @Transactional
    public void unlink(UUID userId, UUID equipmentId, UUID exerciseId) {
        Equipment item = equipmentService.getActive(equipmentId);
        access.requireMember(userId, item.getGym().getId());
        links.findByEquipmentIdAndExerciseId(equipmentId, exerciseId).ifPresent(links::delete);
    }

    private Exercise getVisible(UUID userId, UUID id) {
        Exercise e = exercises.findWithDetails(id).orElseThrow(() -> new NotFoundException("Exercise"));
        if (e.getScope() == ExerciseScope.CUSTOM
                && (!access.isMember(userId, e.getGym().getId()) || !e.isVisibleTo(userId, e.getGym().getId()))) {
            throw new NotFoundException("Exercise");
        }
        return e;
    }

    private Exercise getOwnCustom(UUID userId, UUID id) {
        Exercise e = getVisible(userId, id);
        if (e.getScope() != ExerciseScope.CUSTOM || !e.isAuthor(userId)) {
            throw new ForbiddenException("exercise_not_owner", "Only the author can change this exercise");
        }
        return e;
    }

    private List<UUID> linkedEquipmentIds(Exercise e) {
        return links.findByExerciseId(e.getId()).stream()
                .filter(l -> !l.getEquipment().isDeleted())
                .map(l -> l.getEquipment().getId())
                .toList();
    }

    private List<Equipment> validatedEquipment(UUID gymId, CreateExerciseRequest request) {
        List<UUID> equipmentIds = request.equipmentIds() == null ? List.of() : request.equipmentIds();
        if (!request.bodyweight() && equipmentIds.isEmpty()) {
            throw new BusinessRuleException("exercise_equipment_required",
                    "Choose equipment for the exercise or mark it as bodyweight");
        }
        return equipmentIds.stream().distinct().map(id -> equipment.findActiveById(id)
                        .filter(e -> e.getGym().getId().equals(gymId))
                        .orElseThrow(() -> new BusinessRuleException("equipment_not_in_gym",
                                "Equipment does not belong to this gym")))
                .toList();
    }

    private static Exercise.Details details(CreateExerciseRequest request) {
        Set<MuscleGroup> secondary = request.secondaryMuscles() == null || request.secondaryMuscles().isEmpty()
                ? EnumSet.noneOf(MuscleGroup.class) : EnumSet.copyOf(request.secondaryMuscles());
        return new Exercise.Details(request.name().trim(), request.primaryMuscle(), secondary,
                blankToNull(request.description()), request.bodyweight(), visibility(request));
    }

    private static ExerciseVisibility visibility(CreateExerciseRequest request) {
        return request.visibility() == null ? ExerciseVisibility.PRIVATE : request.visibility();
    }

    private static Stream<Exercise> filter(Stream<Exercise> stream, String q, MuscleGroup muscle) {
        String needle = Objects.requireNonNullElse(TextNormalizer.normalize(q), "");
        return stream
                .filter(e -> needle.isEmpty() || e.getNormalizedName().contains(needle))
                .filter(e -> muscle == null || e.getPrimaryMuscle() == muscle
                        || e.getSecondaryMuscles().contains(muscle));
    }

    private static ExerciseRequirement requirement(Exercise e) {
        return new ExerciseRequirement(e.getId(), e.isBodyweight(),
                e.getEquipmentTypes().stream().map(t -> t.getId()).collect(Collectors.toSet()));
    }

    private static GymEquipment gymEquipment(Equipment e) {
        return new GymEquipment(e.getId(), e.getEquipmentType() == null ? null : e.getEquipmentType().getId(),
                e.isAvailable());
    }

    private static EquipmentOptionDto toOption(Equipment e) {
        return new EquipmentOptionDto(e.getId(), e.getName(),
                FileController.thumbnailUrl(e.getPhoto() == null ? null : e.getPhoto().getId()));
    }

    /** Zapytanie {@code in :gymIds} wymaga niepustej kolekcji. */
    private static Set<UUID> withPlaceholder(Set<UUID> gymIds) {
        return gymIds.isEmpty() ? Set.of(new UUID(0, 0)) : gymIds;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
