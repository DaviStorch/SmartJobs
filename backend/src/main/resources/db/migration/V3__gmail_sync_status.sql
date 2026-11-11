ALTER TABLE gmail_connection
    ADD COLUMN sync_status VARCHAR(20) NOT NULL DEFAULT 'IDLE',
    ADD COLUMN sync_started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_sync_error VARCHAR(500),
    ADD COLUMN last_sync_scanned INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_sync_imported INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_sync_duplicates INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_sync_ignored INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_sync_received_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE gmail_imported_message
    ADD COLUMN sender VARCHAR(320),
    ADD COLUMN received_at TIMESTAMP WITH TIME ZONE;
