package com.gymplanner.gym;

import com.gymplanner.auth.AuthUser;
import com.gymplanner.common.web.PageResponse;
import com.gymplanner.gym.dto.CreateGymRequest;
import com.gymplanner.gym.dto.GymDto;
import com.gymplanner.gym.dto.GymSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Gyms")
public class GymController {

    private final GymService gymService;

    public GymController(GymService gymService) {
        this.gymService = gymService;
    }

    @GetMapping("/gyms")
    @Operation(summary = "Wyszukiwanie siłowni po nazwie i mieście")
    public PageResponse<GymSummaryDto> search(AuthUser user,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @ParameterObject @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return gymService.search(user.id(), q, city, pageable);
    }

    @GetMapping("/gyms/similar")
    @Operation(summary = "Siłownie o podobnej nazwie w danym mieście (ostrzeżenie o duplikacie)")
    public List<GymSummaryDto> similar(AuthUser user, @RequestParam String name, @RequestParam String city) {
        return gymService.findSimilar(user.id(), name, city);
    }

    @PostMapping("/gyms")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Dodanie siłowni. 409 gym_possible_duplicate (z listą candidates), "
            + "jeśli istnieje podobna – ponów z confirmDuplicate=true")
    public GymDto create(AuthUser user, @Valid @RequestBody CreateGymRequest request) {
        return gymService.create(user.id(), request);
    }

    @GetMapping("/gyms/{gymId}")
    public GymDto get(AuthUser user, @PathVariable UUID gymId) {
        return gymService.get(user.id(), gymId);
    }

    @PostMapping("/gyms/{gymId}/membership")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Dołączenie do siłowni (idempotentne)")
    public void join(AuthUser user, @PathVariable UUID gymId) {
        gymService.join(user.id(), gymId);
    }

    @DeleteMapping("/gyms/{gymId}/membership")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(AuthUser user, @PathVariable UUID gymId) {
        gymService.leave(user.id(), gymId);
    }

    @GetMapping("/me/gyms")
    @Operation(summary = "Moje siłownie")
    public List<GymSummaryDto> myGyms(AuthUser user) {
        return gymService.myGyms(user.id());
    }
}
