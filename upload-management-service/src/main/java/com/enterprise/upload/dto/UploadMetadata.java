package com.enterprise.upload.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadMetadata {
    
    @NotBlank(message = "Department is required")
    private String department;
    
    @NotBlank(message = "Access level is required")
    private String accessLevel;
    
    @NotBlank(message = "Dataset type is required")
    private String datasetType;
    
    @NotBlank(message = "Target database is required")
    private String targetDatabase;
    
    private String[] tags;
    private String description;
    private Boolean autoIngest = false;

    // --- Getters and Setters ---

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getAccessLevel() { return accessLevel; }
    public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }

    public String getDatasetType() { return datasetType; }
    public void setDatasetType(String datasetType) { this.datasetType = datasetType; }

    public String getTargetDatabase() { return targetDatabase; }
    public void setTargetDatabase(String targetDatabase) { this.targetDatabase = targetDatabase; }

    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getAutoIngest() { return autoIngest; }
    public void setAutoIngest(Boolean autoIngest) { this.autoIngest = autoIngest; }
}