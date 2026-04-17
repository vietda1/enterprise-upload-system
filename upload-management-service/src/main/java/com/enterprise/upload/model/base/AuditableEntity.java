package com.enterprise.upload.model.base;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @MappedSuperclass
public abstract class AuditableEntity extends BaseEntity {
    @Column(name = "created_by", length = 200)
    private String createdBy;
    @Column(name = "updated_by", length = 200)
    private String updatedBy;
}
