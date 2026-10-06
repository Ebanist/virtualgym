package com.gymplanner.equipment;

import com.gymplanner.equipment.dto.EquipmentChangeDto;
import com.gymplanner.equipment.dto.EquipmentDto;
import com.gymplanner.equipment.dto.EquipmentRefDto;
import com.gymplanner.equipment.dto.EquipmentReportDto;
import com.gymplanner.equipment.dto.EquipmentSummaryDto;
import com.gymplanner.equipment.dto.EquipmentTypeDto;
import com.gymplanner.storage.FileController;
import com.gymplanner.storage.StoredFile;
import com.gymplanner.user.dto.UserRefDto;
import java.util.UUID;

final class EquipmentMapper {

    private EquipmentMapper() {
    }

    static EquipmentSummaryDto toSummary(Equipment e, long openReports) {
        return new EquipmentSummaryDto(e.getId(), e.getName(), e.getCategory(),
                EquipmentTypeDto.from(e.getEquipmentType()), e.getQuantity(), e.getStatus(),
                FileController.thumbnailUrl(photoId(e)), openReports);
    }

    static EquipmentDto toDto(Equipment e, long openReports, boolean member) {
        return new EquipmentDto(e.getId(), e.getGym().getId(), e.getGym().getName(), e.getName(), e.getCategory(),
                EquipmentTypeDto.from(e.getEquipmentType()), e.getDescription(), e.getQuantity(), e.getStatus(),
                e.getSource(), e.isVerified(), FileController.url(photoId(e)),
                FileController.thumbnailUrl(photoId(e)), UserRefDto.from(e.getCreatedBy()), e.getCreatedAt(),
                e.getUpdatedAt(), e.getVersion(), openReports, member);
    }

    static EquipmentChangeDto toDto(EquipmentChange c) {
        return new EquipmentChangeDto(c.getId(), UserRefDto.from(c.getUser()), c.getCreatedAt(), c.getChangeType(),
                c.getChanges());
    }

    static EquipmentReportDto toDto(EquipmentReport r) {
        Equipment dup = r.getDuplicateOf();
        return new EquipmentReportDto(r.getId(), r.getEquipment().getId(), r.getType(), r.getComment(),
                dup == null ? null : new EquipmentRefDto(dup.getId(), dup.getName()), r.getStatus(),
                UserRefDto.from(r.getReporter()), r.getCreatedAt(), UserRefDto.from(r.getResolvedBy()),
                r.getResolvedAt());
    }

    private static UUID photoId(Equipment e) {
        StoredFile photo = e.getPhoto();
        return photo == null ? null : photo.getId();
    }
}
