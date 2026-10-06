package com.gymplanner.equipment;

import com.gymplanner.auth.AuthUser;
import com.gymplanner.common.web.PageResponse;
import com.gymplanner.equipment.dto.CreateEquipmentRequest;
import com.gymplanner.equipment.dto.CreateReportRequest;
import com.gymplanner.equipment.dto.EquipmentChangeDto;
import com.gymplanner.equipment.dto.EquipmentDto;
import com.gymplanner.equipment.dto.EquipmentReportDto;
import com.gymplanner.equipment.dto.EquipmentSummaryDto;
import com.gymplanner.equipment.dto.EquipmentTypeDto;
import com.gymplanner.equipment.dto.UpdateEquipmentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Equipment")
@Validated
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final EquipmentReportService reportService;

    public EquipmentController(EquipmentService equipmentService, EquipmentReportService reportService) {
        this.equipmentService = equipmentService;
        this.reportService = reportService;
    }

    @GetMapping("/equipment-types")
    @Operation(summary = "Słownik typów sprzętu")
    public List<EquipmentTypeDto> types() {
        return equipmentService.listTypes();
    }

    @GetMapping("/gyms/{gymId}/equipment")
    @Operation(summary = "Sprzęt w siłowni – wyszukiwanie po nazwie, filtr kategorii i statusu")
    public PageResponse<EquipmentSummaryDto> list(@PathVariable UUID gymId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EquipmentCategory category,
            @RequestParam(required = false) EquipmentStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return equipmentService.search(gymId, q, category, status, pageable);
    }

    @GetMapping("/gyms/{gymId}/equipment/similar")
    @Operation(summary = "Sprzęt o podobnej nazwie w siłowni (ograniczanie duplikatów)")
    public List<EquipmentSummaryDto> similar(@PathVariable UUID gymId, @RequestParam String name) {
        return equipmentService.findSimilar(gymId, name);
    }

    @PostMapping("/gyms/{gymId}/equipment")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Dodanie sprzętu (tylko członkowie siłowni)")
    public EquipmentDto create(AuthUser user, @PathVariable UUID gymId,
            @Valid @RequestBody CreateEquipmentRequest request) {
        return equipmentService.create(user.id(), gymId, request);
    }

    @GetMapping("/equipment/{id}")
    public EquipmentDto get(AuthUser user, @PathVariable UUID id) {
        return equipmentService.get(user.id(), id);
    }

    @PutMapping("/equipment/{id}")
    @Operation(summary = "Edycja sprzętu (członkowie siłowni); zmiana statusu na REMOVED_FROM_GYM też tutaj")
    public EquipmentDto update(AuthUser user, @PathVariable UUID id,
            @Valid @RequestBody UpdateEquipmentRequest request) {
        return equipmentService.update(user.id(), id, request);
    }

    @PostMapping(path = "/equipment/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Dodanie/zmiana zdjęcia (jpg/png/webp, max 5 MB)")
    public EquipmentDto uploadPhoto(AuthUser user, @PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return equipmentService.uploadPhoto(user.id(), id, file);
    }

    @DeleteMapping("/equipment/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Usunięcie sprzętu (miękkie)")
    public void delete(AuthUser user, @PathVariable UUID id) {
        equipmentService.delete(user.id(), id);
    }

    @GetMapping("/equipment/{id}/history")
    @Operation(summary = "Historia zmian sprzętu (od najnowszych)")
    public PageResponse<EquipmentChangeDto> history(@PathVariable UUID id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return equipmentService.history(id, page, size);
    }

    @GetMapping("/equipment/{id}/reports")
    @Operation(summary = "Zgłoszenia problemów (otwarte najpierw)")
    public List<EquipmentReportDto> reports(@PathVariable UUID id) {
        return reportService.list(id);
    }

    @PostMapping("/equipment/{id}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentReportDto report(AuthUser user, @PathVariable UUID id,
            @Valid @RequestBody CreateReportRequest request) {
        return reportService.create(user.id(), id, request);
    }

    @PostMapping("/reports/{reportId}/resolve")
    @Operation(summary = "Oznaczenie zgłoszenia jako rozwiązanego (członkowie siłowni)")
    public EquipmentReportDto resolve(AuthUser user, @PathVariable UUID reportId) {
        return reportService.resolve(user.id(), reportId);
    }
}
