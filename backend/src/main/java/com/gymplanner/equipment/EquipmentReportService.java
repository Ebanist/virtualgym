package com.gymplanner.equipment;

import com.gymplanner.common.error.BusinessRuleException;
import com.gymplanner.common.error.ConflictException;
import com.gymplanner.common.error.NotFoundException;
import com.gymplanner.equipment.dto.CreateReportRequest;
import com.gymplanner.equipment.dto.EquipmentReportDto;
import com.gymplanner.gym.GymAccessService;
import com.gymplanner.user.UserRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentReportService {

    private final EquipmentReportRepository reports;
    private final EquipmentRepository equipment;
    private final EquipmentService equipmentService;
    private final GymAccessService access;
    private final UserRepository users;
    private final Clock clock;

    public EquipmentReportService(EquipmentReportRepository reports, EquipmentRepository equipment,
            EquipmentService equipmentService, GymAccessService access, UserRepository users, Clock clock) {
        this.reports = reports;
        this.equipment = equipment;
        this.equipmentService = equipmentService;
        this.access = access;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<EquipmentReportDto> list(UUID equipmentId) {
        equipmentService.getActive(equipmentId);
        return reports.findForEquipment(equipmentId).stream().map(EquipmentMapper::toDto).toList();
    }

    @Transactional
    public EquipmentReportDto create(UUID userId, UUID equipmentId, CreateReportRequest request) {
        Equipment e = equipmentService.getActive(equipmentId);
        access.requireMember(userId, e.getGym().getId());
        if (reports.existsByEquipmentIdAndReporterIdAndTypeAndStatus(equipmentId, userId, request.type(),
                ReportStatus.OPEN)) {
            throw new ConflictException("report_already_open", "You already reported this problem");
        }
        Equipment duplicateOf = null;
        if (request.duplicateOfId() != null) {
            if (request.type() != ReportType.DUPLICATE || request.duplicateOfId().equals(equipmentId)) {
                throw new BusinessRuleException("invalid_duplicate_reference", "Invalid duplicate reference");
            }
            duplicateOf = equipment.findActiveById(request.duplicateOfId())
                    .filter(d -> d.getGym().getId().equals(e.getGym().getId()))
                    .orElseThrow(() -> new BusinessRuleException("invalid_duplicate_reference",
                            "Duplicate must be equipment from the same gym"));
        }
        String comment = request.comment() == null || request.comment().isBlank() ? null : request.comment().trim();
        EquipmentReport report = reports.save(new EquipmentReport(e, users.getReferenceById(userId), request.type(),
                comment, duplicateOf));
        return EquipmentMapper.toDto(report);
    }

    @Transactional
    public EquipmentReportDto resolve(UUID userId, UUID reportId) {
        EquipmentReport report = reports.findById(reportId).orElseThrow(() -> new NotFoundException("Report"));
        access.requireMember(userId, report.getEquipment().getGym().getId());
        if (report.getStatus() == ReportStatus.OPEN) {
            report.resolve(users.getReferenceById(userId), clock.instant());
        }
        return EquipmentMapper.toDto(report);
    }
}
