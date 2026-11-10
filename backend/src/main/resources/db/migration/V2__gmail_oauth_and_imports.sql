CREATE TABLE gmail_connection (
    user_id BIGINT PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
    access_token_ciphertext TEXT NOT NULL,
    refresh_token_ciphertext TEXT,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    connected_email VARCHAR(254),
    last_synced_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE gmail_imported_message (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    message_id VARCHAR(200) NOT NULL,
    result VARCHAR(20) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_gmail_message_user_message UNIQUE (user_id, message_id)
);
