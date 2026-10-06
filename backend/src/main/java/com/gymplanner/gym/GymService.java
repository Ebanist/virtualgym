package com.gymplanner.gym;

import com.gymplanner.common.error.ConflictException;
import com.gymplanner.common.text.TextNormalizer;
import com.gymplanner.common.web.PageResponse;
import com.gymplanner.gym.dto.CreateGymRequest;
import com.gymplanner.gym.dto.GymDto;
import com.gymplanner.gym.dto.GymSummaryDto;
import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GymService {

    /** Próg podobieństwa trigramów (pg_trgm), powyżej którego ostrzegamy o możliwym duplikacie. */
    static final double SIMILARITY_THRESHOLD = 0.4;
    private static final int SIMILAR_LIMIT = 5;

    private final GymRepository gyms;
    private final GymMembershipRepository memberships;
    private final GymAccessService access;
    private final UserRepository users;

    public GymService(GymRepository gyms, GymMembershipRepository memberships, GymAccessService access,
            UserRepository users) {
        this.gyms = gyms;
        this.memberships = memberships;
        this.access = access;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public PageResponse<GymSummaryDto> search(UUID userId, String name, String city, Pageable pageable) {
        Page<Gym> page = gyms.search(normalizeOrEmpty(name), normalizeOrEmpty(city), pageable);
        return PageResponse.of(page, summaryMapper(userId, page.getContent()));
    }

    @Transactional(readOnly = true)
    public List<GymSummaryDto> findSimilar(UUID userId, String name, String city) {
        String normalizedName = TextNormalizer.normalize(name);
        String normalizedCity = TextNormalizer.normalize(city);
        if (normalizedName == null || normalizedName.length() < 2 || normalizedCity == null
                || normalizedCity.isEmpty()) {
            return List.of();
        }
        List<Gym> similar = gyms.findSimilar(normalizedName, normalizedCity, SIMILARITY_THRESHOLD, SIMILAR_LIMIT);
        return similar.stream().map(summaryMapper(userId, similar)).toList();
    }

    @Transactional
    public GymDto create(UUID userId, CreateGymRequest request) {
        if (!request.confirmDuplicate()) {
            List<GymSummaryDto> similar = findSimilar(userId, request.name(), request.city());
            if (!similar.isEmpty()) {
                throw new ConflictException("gym_possible_duplicate",
                        "A gym with a similar name already exists in this city", Map.of("candidates", similar));
            }
        }
        User user = users.getReferenceById(userId);
        Gym gym = gyms.save(new Gym(request.name().trim(), request.city().trim(), request.address().trim(),
                blankToNull(request.description()), user));
        // Autor siłowni automatycznie do niej dołącza – może od razu dodawać sprzęt.
        memberships.save(new GymMembership(user, gym));
        return toDto(gym, 1, true);
    }

    @Transactional(readOnly = true)
    public GymDto get(UUID userId, UUID gymId) {
        Gym gym = access.getGym(gymId);
        return toDto(gym, memberships.countByGymId(gymId), access.isMember(userId, gymId));
    }

    @Transactional
    public void join(UUID userId, UUID gymId) {
        Gym gym = access.getGym(gymId);
        if (!access.isMember(userId, gymId)) {
            memberships.save(new GymMembership(users.getReferenceById(userId), gym));
        }
    }

    @Transactional
    public void leave(UUID userId, UUID gymId) {
        access.getGym(gymId);
        memberships.findByUserIdAndGymId(userId, gymId).ifPresent(memberships::delete);
    }

    @Transactional(readOnly = true)
    public List<GymSummaryDto> myGyms(UUID userId) {
        List<Gym> mine = memberships.findByUserIdWithGym(userId).stream().map(GymMembership::getGym).toList();
        return mine.stream().map(summaryMapper(userId, mine)).toList();
    }

    /** Mapper liczący członków i członkostwo dwoma zapytaniami dla całej strony wyników (bez N+1). */
    private java.util.function.Function<Gym, GymSummaryDto> summaryMapper(UUID userId, List<Gym> page) {
        if (page.isEmpty()) {
            return gym -> toSummary(gym, 0, false);
        }
        Set<UUID> ids = page.stream().map(Gym::getId).collect(Collectors.toSet());
        Map<UUID, Long> counts = memberships.countByGymIds(ids).stream()
                .collect(Collectors.toMap(GymMembershipRepository.GymMemberCount::getGymId,
                        GymMembershipRepository.GymMemberCount::getCount));
        Set<UUID> memberOf = new HashSet<>(memberships.findGymIdsOfUser(userId, ids));
        return gym -> toSummary(gym, counts.getOrDefault(gym.getId(), 0L), memberOf.contains(gym.getId()));
    }

    private static GymSummaryDto toSummary(Gym gym, long memberCount, boolean member) {
        return new GymSummaryDto(gym.getId(), gym.getName(), gym.getCity(), gym.getAddress(), gym.getStatus(),
                memberCount, member);
    }

    private static GymDto toDto(Gym gym, long memberCount, boolean member) {
        return new GymDto(gym.getId(), gym.getName(), gym.getCity(), gym.getAddress(), gym.getDescription(),
                gym.getStatus(), memberCount, member, gym.getCreatedAt());
    }

    private static String normalizeOrEmpty(String value) {
        String normalized = TextNormalizer.normalize(value);
        return normalized == null ? "" : normalized;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
