-- ============================================================================
-- UPLOAD MANAGEMENT SERVICE - DATABASE SCHEMA
-- PostgreSQL 15+
-- ============================================================================

-- ============================================================================
-- 1. DATABASE CREATION
-- ============================================================================

-- Create database (run as superuser)
CREATE DATABASE upload_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Connect to database
\c upload_db;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- For full-text search

-- ============================================================================
-- 2. MAIN TABLES
-- ============================================================================

-- Uploads table
CREATE TABLE uploads (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size BIGINT,
    object_key VARCHAR(1000) NOT NULL UNIQUE,
    bucket_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    department VARCHAR(100) NOT NULL,
    access_level VARCHAR(50) NOT NULL,
    dataset_type VARCHAR(100) NOT NULL,
    target_database VARCHAR(500) NOT NULL,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploaded_at TIMESTAMP,
    validated_at TIMESTAMP,
    ingested_at TIMESTAMP,
    
    -- Results
    validation_result TEXT,
    ingestion_result TEXT,
    error_message VARCHAR(500),
    
    -- Optimistic locking
    version BIGINT NOT NULL DEFAULT 0,
    
    -- Constraints
    CONSTRAINT chk_status CHECK (
        status IN (
            'PENDING', 'UPLOADING', 'UPLOADED', 'VALIDATING', 
            'VALIDATION_FAILED', 'VALID', 'INGESTING', 
            'INGESTION_FAILED', 'COMPLETED', 'DELETED', 'FAILED'
        )
    ),
    CONSTRAINT chk_access_level CHECK (
        access_level IN ('PRIVATE', 'SHARED', 'PUBLIC')
    ),
    CONSTRAINT chk_file_size CHECK (file_size >= 0)
);

-- Upload metadata table (for flexible key-value pairs)
CREATE TABLE upload_metadata (
    upload_id VARCHAR(36) NOT NULL,
    meta_key VARCHAR(255) NOT NULL,
    meta_value TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (upload_id, meta_key),
    CONSTRAINT fk_upload_metadata_upload 
        FOREIGN KEY (upload_id) 
        REFERENCES uploads(id) 
        ON DELETE CASCADE
);

-- Upload history table (for audit trail)
CREATE TABLE upload_history (
    id BIGSERIAL PRIMARY KEY,
    upload_id VARCHAR(36) NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by VARCHAR(255),
    change_reason VARCHAR(500),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_upload_history_upload 
        FOREIGN KEY (upload_id) 
        REFERENCES uploads(id) 
        ON DELETE CASCADE
);

-- Dataset configurations table
CREATE TABLE dataset_configs (
    id SERIAL PRIMARY KEY,
    type VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    default_database VARCHAR(500) NOT NULL,
    target_table VARCHAR(200),
    target_collection VARCHAR(200),
    validation_rules JSONB,
    schema_definition JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Upload tags table (for categorization)
CREATE TABLE upload_tags (
    id SERIAL PRIMARY KEY,
    upload_id VARCHAR(36) NOT NULL,
    tag VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_upload_tags_upload 
        FOREIGN KEY (upload_id) 
        REFERENCES uploads(id) 
        ON DELETE CASCADE,
    CONSTRAINT uk_upload_tag UNIQUE (upload_id, tag)
);

-- Upload shares table (for explicit sharing)
CREATE TABLE upload_shares (
    id SERIAL PRIMARY KEY,
    upload_id VARCHAR(36) NOT NULL,
    shared_with_user_id VARCHAR(255),
    shared_with_department VARCHAR(100),
    permission VARCHAR(50) NOT NULL DEFAULT 'READ',
    shared_by VARCHAR(255) NOT NULL,
    shared_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    
    CONSTRAINT fk_upload_shares_upload 
        FOREIGN KEY (upload_id) 
        REFERENCES uploads(id) 
        ON DELETE CASCADE,
    CONSTRAINT chk_permission CHECK (
        permission IN ('READ', 'WRITE', 'DELETE')
    ),
    CONSTRAINT chk_share_target CHECK (
        (shared_with_user_id IS NOT NULL AND shared_with_department IS NULL) OR
        (shared_with_user_id IS NULL AND shared_with_department IS NOT NULL)
    )
);

-- ============================================================================
-- 3. INDEXES
-- ============================================================================

-- Uploads table indexes
CREATE INDEX idx_uploads_user_id ON uploads(user_id);
CREATE INDEX idx_uploads_status ON uploads(status);
CREATE INDEX idx_uploads_department ON uploads(department);
CREATE INDEX idx_uploads_dataset_type ON uploads(dataset_type);
CREATE INDEX idx_uploads_access_level ON uploads(access_level);
CREATE INDEX idx_uploads_created_at ON uploads(created_at DESC);
CREATE INDEX idx_uploads_updated_at ON uploads(updated_at DESC);
CREATE INDEX idx_uploads_user_status ON uploads(user_id, status);
CREATE INDEX idx_uploads_user_dept ON uploads(user_id, department);
CREATE INDEX idx_uploads_dept_status ON uploads(department, status);

-- Full-text search index for file names
CREATE INDEX idx_uploads_filename_trgm ON uploads USING gin (file_name gin_trgm_ops);

-- Metadata indexes
CREATE INDEX idx_upload_metadata_key ON upload_metadata(meta_key);
CREATE INDEX idx_upload_metadata_upload_id ON upload_metadata(upload_id);

-- History indexes
CREATE INDEX idx_upload_history_upload_id ON upload_history(upload_id);
CREATE INDEX idx_upload_history_changed_at ON upload_history(changed_at DESC);

-- Tags indexes
CREATE INDEX idx_upload_tags_upload_id ON upload_tags(upload_id);
CREATE INDEX idx_upload_tags_tag ON upload_tags(tag);

-- Shares indexes
CREATE INDEX idx_upload_shares_upload_id ON upload_shares(upload_id);
CREATE INDEX idx_upload_shares_user ON upload_shares(shared_with_user_id);
CREATE INDEX idx_upload_shares_dept ON upload_shares(shared_with_department);

-- ============================================================================
-- 4. FUNCTIONS & TRIGGERS
-- ============================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for uploads table
CREATE TRIGGER trg_uploads_updated_at
    BEFORE UPDATE ON uploads
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger for dataset_configs table
CREATE TRIGGER trg_dataset_configs_updated_at
    BEFORE UPDATE ON dataset_configs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Function to log upload status changes
CREATE OR REPLACE FUNCTION log_upload_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'UPDATE' AND OLD.status IS DISTINCT FROM NEW.status) THEN
        INSERT INTO upload_history (
            upload_id, 
            old_status, 
            new_status, 
            changed_by,
            change_reason
        ) VALUES (
            NEW.id,
            OLD.status,
            NEW.status,
            COALESCE(current_setting('app.current_user', TRUE), 'SYSTEM'),
            CASE 
                WHEN NEW.status = 'UPLOADED' THEN 'File uploaded to storage'
                WHEN NEW.status = 'VALID' THEN 'Validation passed'
                WHEN NEW.status = 'VALIDATION_FAILED' THEN 'Validation failed'
                WHEN NEW.status = 'COMPLETED' THEN 'Ingestion completed'
                WHEN NEW.status = 'FAILED' THEN 'Upload failed'
                ELSE 'Status changed'
            END
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for upload status changes
CREATE TRIGGER trg_log_upload_status_change
    AFTER UPDATE ON uploads
    FOR EACH ROW
    EXECUTE FUNCTION log_upload_status_change();

-- ============================================================================
-- 5. VIEWS
-- ============================================================================

-- View for upload statistics by user
CREATE OR REPLACE VIEW v_upload_stats_by_user AS
SELECT 
    user_id,
    COUNT(*) as total_uploads,
    COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending_uploads,
    COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_uploads,
    COUNT(CASE WHEN status IN ('FAILED', 'VALIDATION_FAILED', 'INGESTION_FAILED') THEN 1 END) as failed_uploads,
    COALESCE(SUM(file_size), 0) as total_size_bytes,
    COALESCE(AVG(file_size), 0) as avg_size_bytes,
    MAX(created_at) as last_upload_at
FROM uploads
WHERE status != 'DELETED'
GROUP BY user_id;

-- View for upload statistics by department
CREATE OR REPLACE VIEW v_upload_stats_by_department AS
SELECT 
    department,
    COUNT(*) as total_uploads,
    COUNT(DISTINCT user_id) as total_users,
    COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_uploads,
    COUNT(CASE WHEN status IN ('FAILED', 'VALIDATION_FAILED', 'INGESTION_FAILED') THEN 1 END) as failed_uploads,
    COALESCE(SUM(file_size), 0) as total_size_bytes,
    MAX(created_at) as last_upload_at
FROM uploads
WHERE status != 'DELETED'
GROUP BY department;

-- View for upload statistics by dataset type
CREATE OR REPLACE VIEW v_upload_stats_by_dataset AS
SELECT 
    dataset_type,
    COUNT(*) as total_uploads,
    COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_uploads,
    COUNT(CASE WHEN status = 'VALIDATION_FAILED' THEN 1 END) as validation_failed,
    COUNT(CASE WHEN status = 'INGESTION_FAILED' THEN 1 END) as ingestion_failed,
    COALESCE(AVG(
        EXTRACT(EPOCH FROM (ingested_at - created_at))
    ), 0) as avg_processing_time_seconds
FROM uploads
WHERE status != 'DELETED'
GROUP BY dataset_type;

-- View for recent uploads with metadata
CREATE OR REPLACE VIEW v_recent_uploads AS
SELECT 
    u.id,
    u.user_id,
    u.file_name,
    u.file_size,
    u.status,
    u.department,
    u.dataset_type,
    u.created_at,
    u.uploaded_at,
    u.validated_at,
    u.ingested_at,
    ARRAY_AGG(DISTINCT t.tag) FILTER (WHERE t.tag IS NOT NULL) as tags,
    COUNT(DISTINCT s.id) as share_count
FROM uploads u
LEFT JOIN upload_tags t ON u.id = t.upload_id
LEFT JOIN upload_shares s ON u.id = s.upload_id AND (s.expires_at IS NULL OR s.expires_at > CURRENT_TIMESTAMP)
WHERE u.status != 'DELETED'
GROUP BY u.id
ORDER BY u.created_at DESC
LIMIT 100;

-- ============================================================================
-- 6. STORED PROCEDURES
-- ============================================================================

-- Procedure to get accessible uploads for a user
CREATE OR REPLACE FUNCTION get_accessible_uploads(
    p_user_id VARCHAR(255),
    p_department VARCHAR(100),
    p_limit INTEGER DEFAULT 20,
    p_offset INTEGER DEFAULT 0
)
RETURNS TABLE (
    id VARCHAR(36),
    user_id VARCHAR(255),
    file_name VARCHAR(500),
    file_type VARCHAR(100),
    file_size BIGINT,
    status VARCHAR(50),
    department VARCHAR(100),
    access_level VARCHAR(50),
    created_at TIMESTAMP,
    access_type VARCHAR(20)
) AS $$
BEGIN
    RETURN QUERY
    SELECT DISTINCT
        u.id,
        u.user_id,
        u.file_name,
        u.file_type,
        u.file_size,
        u.status,
        u.department,
        u.access_level,
        u.created_at,
        CASE 
            WHEN u.user_id = p_user_id THEN 'OWNER'
            WHEN u.access_level = 'PUBLIC' THEN 'PUBLIC'
            WHEN u.access_level = 'SHARED' AND u.department = p_department THEN 'DEPARTMENT'
            WHEN us.id IS NOT NULL THEN 'EXPLICIT_SHARE'
            ELSE 'NONE'
        END as access_type
    FROM uploads u
    LEFT JOIN upload_shares us ON u.id = us.upload_id 
        AND (us.shared_with_user_id = p_user_id OR us.shared_with_department = p_department)
        AND (us.expires_at IS NULL OR us.expires_at > CURRENT_TIMESTAMP)
    WHERE u.status != 'DELETED'
        AND (
            u.user_id = p_user_id
            OR u.access_level = 'PUBLIC'
            OR (u.access_level = 'SHARED' AND u.department = p_department)
            OR us.id IS NOT NULL
        )
    ORDER BY u.created_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- Procedure to cleanup old uploads
CREATE OR REPLACE FUNCTION cleanup_old_uploads(
    p_days_old INTEGER DEFAULT 90
)
RETURNS TABLE (
    deleted_count INTEGER
) AS $$
DECLARE
    v_deleted_count INTEGER;
BEGIN
    WITH deleted AS (
        DELETE FROM uploads
        WHERE status = 'DELETED'
            AND updated_at < CURRENT_TIMESTAMP - (p_days_old || ' days')::INTERVAL
        RETURNING id
    )
    SELECT COUNT(*) INTO v_deleted_count FROM deleted;
    
    RETURN QUERY SELECT v_deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Procedure to get upload processing metrics
CREATE OR REPLACE FUNCTION get_upload_metrics(
    p_start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP - INTERVAL '7 days',
    p_end_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
RETURNS TABLE (
    metric_name VARCHAR(100),
    metric_value NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT 'total_uploads'::VARCHAR(100), COUNT(*)::NUMERIC
    FROM uploads
    WHERE created_at BETWEEN p_start_date AND p_end_date
    UNION ALL
    SELECT 'completed_uploads'::VARCHAR(100), COUNT(*)::NUMERIC
    FROM uploads
    WHERE created_at BETWEEN p_start_date AND p_end_date
        AND status = 'COMPLETED'
    UNION ALL
    SELECT 'failed_uploads'::VARCHAR(100), COUNT(*)::NUMERIC
    FROM uploads
    WHERE created_at BETWEEN p_start_date AND p_end_date
        AND status IN ('FAILED', 'VALIDATION_FAILED', 'INGESTION_FAILED')
    UNION ALL
    SELECT 'avg_processing_time_seconds'::VARCHAR(100), 
           COALESCE(AVG(EXTRACT(EPOCH FROM (ingested_at - created_at))), 0)::NUMERIC
    FROM uploads
    WHERE created_at BETWEEN p_start_date AND p_end_date
        AND status = 'COMPLETED'
        AND ingested_at IS NOT NULL
    UNION ALL
    SELECT 'total_size_bytes'::VARCHAR(100), COALESCE(SUM(file_size), 0)::NUMERIC
    FROM uploads
    WHERE created_at BETWEEN p_start_date AND p_end_date;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 7. SEED DATA
-- ============================================================================

-- Insert dataset configurations
INSERT INTO dataset_configs (type, name, description, default_database, target_table, validation_rules) VALUES
(
    'CSV_TRANSACTION',
    'Transaction Data',
    'Financial transaction records in CSV format',
    'postgresql://transaction-db:5432/transactions',
    'transactions',
    '{
        "required_columns": ["transaction_id", "amount", "date", "customer_id"],
        "column_types": {
            "transaction_id": "string",
            "amount": "numeric",
            "date": "datetime",
            "customer_id": "string"
        },
        "constraints": {
            "amount": {"min": 0, "max": 1000000},
            "transaction_id": {"unique": true}
        }
    }'::JSONB
),
(
    'JSON_USER_PROFILE',
    'User Profile Data',
    'User profile information in JSON format',
    'mongodb://mongo:27017/users',
    NULL,
    '{
        "required_fields": ["user_id", "email", "created_at"],
        "field_types": {
            "user_id": "string",
            "email": "string",
            "created_at": "string"
        },
        "constraints": {
            "email": {"pattern": "^[\\w\\.-]+@[\\w\\.-]+\\.\\w+$"}
        }
    }'::JSONB
),
(
    'PARQUET_ANALYTICS',
    'Analytics Events',
    'Analytics event data in Parquet format',
    'clickhouse://clickhouse:8123/analytics',
    'events',
    '{
        "required_columns": ["event_id", "event_type", "timestamp", "user_id"],
        "column_types": {
            "event_id": "string",
            "event_type": "string",
            "timestamp": "datetime",
            "user_id": "string"
        }
    }'::JSONB
),
(
    'CSV_SALES',
    'Sales Data',
    'Sales records in CSV format',
    'postgresql://transaction-db:5432/transactions',
    'sales',
    '{
        "required_columns": ["sale_id", "product_id", "quantity", "price", "sale_date"],
        "column_types": {
            "sale_id": "string",
            "product_id": "string",
            "quantity": "numeric",
            "price": "numeric",
            "sale_date": "datetime"
        },
        "constraints": {
            "quantity": {"min": 1},
            "price": {"min": 0}
        }
    }'::JSONB
),
(
    'CSV_INVENTORY',
    'Inventory Data',
    'Product inventory records',
    'postgresql://transaction-db:5432/transactions',
    'inventory',
    '{
        "required_columns": ["product_id", "quantity", "location", "last_updated"],
        "column_types": {
            "product_id": "string",
            "quantity": "numeric",
            "location": "string",
            "last_updated": "datetime"
        }
    }'::JSONB
);

-- ============================================================================
-- 8. SAMPLE DATA (for testing)
-- ============================================================================

-- Insert sample uploads
INSERT INTO uploads (
    id, user_id, file_name, file_type, file_size, object_key, bucket_name,
    status, department, access_level, dataset_type, target_database
) VALUES
(
    'sample-upload-1',
    'user-123',
    'transactions_2024.csv',
    'text/csv',
    1048576,
    'user-123/finance/sample-upload-1-transactions_2024.csv',
    'enterprise-uploads',
    'COMPLETED',
    'finance',
    'PRIVATE',
    'CSV_TRANSACTION',
    'postgresql://transaction-db:5432/transactions'
),
(
    'sample-upload-2',
    'user-456',
    'user_profiles.json',
    'application/json',
    524288,
    'user-456/tech/sample-upload-2-user_profiles.json',
    'enterprise-uploads',
    'VALID',
    'tech',
    'SHARED',
    'JSON_USER_PROFILE',
    'mongodb://mongo:27017/users'
);

-- Insert sample metadata
INSERT INTO upload_metadata (upload_id, meta_key, meta_value) VALUES
('sample-upload-1', 'description', 'Monthly transaction data for Q4 2024'),
('sample-upload-1', 'tags', 'finance,monthly,q4-2024'),
('sample-upload-1', 'autoIngest', 'true'),
('sample-upload-2', 'description', 'User profile export from CRM'),
('sample-upload-2', 'tags', 'users,crm,export');

-- Insert sample tags
INSERT INTO upload_tags (upload_id, tag) VALUES
('sample-upload-1', 'finance'),
('sample-upload-1', 'monthly'),
('sample-upload-1', 'q4-2024'),
('sample-upload-2', 'users'),
('sample-upload-2', 'crm');

-- ============================================================================
-- 9. GRANTS & PERMISSIONS
-- ============================================================================

-- Create application user
CREATE USER upload_service WITH PASSWORD 'upload_service_password';

-- Grant permissions
GRANT CONNECT ON DATABASE upload_db TO upload_service;
GRANT USAGE ON SCHEMA public TO upload_service;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO upload_service;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO upload_service;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO upload_service;

-- Grant permissions on future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO upload_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO upload_service;

-- ============================================================================
-- 10. MAINTENANCE QUERIES
-- ============================================================================

-- Query to find orphaned metadata
-- SELECT * FROM upload_metadata WHERE upload_id NOT IN (SELECT id FROM uploads);

-- Query to find uploads stuck in processing
-- SELECT * FROM uploads 
-- WHERE status IN ('UPLOADING', 'VALIDATING', 'INGESTING')
--   AND created_at < CURRENT_TIMESTAMP - INTERVAL '1 hour';

-- Query to get storage usage by department
-- SELECT department, 
--        COUNT(*) as upload_count,
--        pg_size_pretty(SUM(file_size)::BIGINT) as total_size
-- FROM uploads
-- WHERE status != 'DELETED'
-- GROUP BY department
-- ORDER BY SUM(file_size) DESC;

-- ============================================================================
-- END OF SCHEMA
-- ============================================================================