package com.tulumcore.api.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tenant_features",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tenant_features_tenant_feature",
                columnNames = {"tenant_id", "feature_key"}
        )
)
public class TenantFeature extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_key", nullable = false, length = 80)
    private FeatureKey featureKey;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "configuration_json", columnDefinition = "text")
    private String configurationJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FeatureKey getFeatureKey() { return featureKey; }
    public void setFeatureKey(FeatureKey featureKey) { this.featureKey = featureKey; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getConfigurationJson() { return configurationJson; }
    public void setConfigurationJson(String configurationJson) { this.configurationJson = configurationJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
