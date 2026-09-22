-- OTT Platform Database Schema
-- Run this on PostgreSQL: psql -U postgres -d ott_platform -f schema.sql

-- Create database
-- CREATE DATABASE ott_platform;
-- CREATE USER ott_user WITH PASSWORD 'your_password';
-- GRANT ALL PRIVILEGES ON DATABASE ott_platform TO ott_user;

-- ==========================================
-- EXTENSIONS
-- ==========================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==========================================
-- EPISODES TABLE
-- ==========================================
CREATE TABLE episodes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    episode_id VARCHAR(255) UNIQUE NOT NULL,  -- e.g., "ep_001", "movie_123"
    title VARCHAR(500) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(1000),
    content_type VARCHAR(50) DEFAULT 'episode', -- 'movie', 'episode', 'live'
    season_number INTEGER,
    episode_number INTEGER,
    duration_seconds INTEGER,
    release_date TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ==========================================
-- STREAMING LINKS TABLE (Multiple links per episode)
-- ==========================================
CREATE TABLE streaming_links (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    episode_id UUID NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    link_id VARCHAR(255) UNIQUE NOT NULL, -- e.g., "ep_001_link_1"
    telegram_file_path VARCHAR(1000) NOT NULL, -- Telegram file path or channel/message ID
    original_hls_url VARCHAR(1000), -- Original HLS URL from Telegram
    quality VARCHAR(50) DEFAULT 'Auto', -- '4K', '1080p', '720p', '480p', 'Auto'
    language VARCHAR(50) DEFAULT 'Hindi', -- 'Hindi', 'English', 'Tamil', etc.
    is_active BOOLEAN DEFAULT true,
    max_concurrent_users INTEGER DEFAULT 250,
    priority INTEGER DEFAULT 0, -- Higher priority = preferred link
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ==========================================
-- REAL-TIME VIEWER TRACKING
-- ==========================================
CREATE TABLE active_viewers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    link_id UUID NOT NULL REFERENCES streaming_links(id) ON DELETE CASCADE,
    user_session_id VARCHAR(255) NOT NULL, -- Anonymous session ID
    user_ip INET,
    user_agent TEXT,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_heartbeat TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(link_id, user_session_id)
);

-- ==========================================
-- VIEWER STATISTICS (Aggregated)
-- ==========================================
CREATE TABLE link_statistics (
    link_id UUID PRIMARY KEY REFERENCES streaming_links(id) ON DELETE CASCADE,
    total_views BIGINT DEFAULT 0,
    total_unique_viewers BIGINT DEFAULT 0,
    total_bandwidth_gb DECIMAL(12,3) DEFAULT 0,
    avg_session_duration_seconds INTEGER DEFAULT 0,
    peak_concurrent_users INTEGER DEFAULT 0,
    last_viewed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ==========================================
-- ADMIN USERS
-- ==========================================
CREATE TABLE admin_users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'admin', -- 'superadmin', 'admin', 'moderator'
    is_active BOOLEAN DEFAULT true,
    last_login TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ==========================================
-- AUDIT LOGS
-- ==========================================
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admin_user_id UUID REFERENCES admin_users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL, -- 'CREATE_EPISODE', 'UPDATE_LINK', 'DELETE_LINK', etc.
    target_type VARCHAR(50), -- 'episode', 'link', 'admin'
    target_id UUID,
    old_data JSONB,
    new_data JSONB,
    ip_address INET,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ==========================================
-- INDEXES FOR PERFORMANCE
-- ==========================================
CREATE INDEX idx_episodes_episode_id ON episodes(episode_id);
CREATE INDEX idx_episodes_active ON episodes(is_active);
CREATE INDEX idx_streaming_links_episode_id ON streaming_links(episode_id);
CREATE INDEX idx_streaming_links_link_id ON streaming_links(link_id);
CREATE INDEX idx_streaming_links_active ON streaming_links(is_active);
CREATE INDEX idx_active_viewers_link_id ON active_viewers(link_id);
CREATE INDEX idx_active_viewers_heartbeat ON active_viewers(last_heartbeat);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- ==========================================
-- TRIGGERS FOR UPDATED_AT
-- ==========================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_episodes_updated_at BEFORE UPDATE ON episodes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_streaming_links_updated_at BEFORE UPDATE ON streaming_links
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ==========================================
-- VIEW FOR ADMIN DASHBOARD
-- ==========================================
CREATE OR REPLACE VIEW episode_dashboard AS
SELECT
    e.id,
    e.episode_id,
    e.title,
    e.content_type,
    e.season_number,
    e.episode_number,
    e.is_active,
    e.created_at,
    COUNT(sl.id) as total_links,
    SUM(CASE WHEN sl.is_active THEN 1 ELSE 0 END) as active_links,
    COALESCE(SUM(ls.active_users), 0) as current_viewers,
    COALESCE(SUM(ls.total_views), 0) as total_views
FROM episodes e
LEFT JOIN streaming_links sl ON sl.episode_id = e.id
LEFT JOIN link_statistics ls ON ls.link_id = sl.id
GROUP BY e.id, e.episode_id, e.title, e.content_type, e.season_number, e.episode_number, e.is_active, e.created_at;

-- ==========================================
-- FUNCTION: Get available link for streaming (smart routing)
-- ==========================================
CREATE OR REPLACE FUNCTION get_available_link(p_episode_id UUID)
RETURNS TABLE (
    link_id UUID,
    link_identifier VARCHAR(255),
    telegram_file_path VARCHAR(1000),
    quality VARCHAR(50),
    language VARCHAR(50),
    current_users INTEGER,
    max_users INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        sl.id as link_id,
        sl.link_id as link_identifier,
        sl.telegram_file_path,
        sl.quality,
        sl.language,
        COALESCE(av.active_count, 0) as current_users,
        sl.max_concurrent_users as max_users
    FROM streaming_links sl
    LEFT JOIN (
        SELECT link_id, COUNT(*) as active_count
        FROM active_viewers
        WHERE last_heartbeat > NOW() - INTERVAL '30 seconds'
        GROUP BY link_id
    ) av ON av.link_id = sl.id
    WHERE sl.episode_id = p_episode_id
      AND sl.is_active = true
      AND COALESCE(av.active_count, 0) < sl.max_concurrent_users
    ORDER BY COALESCE(av.active_count, 0) ASC, sl.priority DESC
    LIMIT 1;
END;
$$ LANGUAGE plpgsql;

-- ==========================================
-- FUNCTION: Release viewer slot
-- ==========================================
CREATE OR REPLACE FUNCTION release_viewer_slot(p_link_id UUID, p_session_id VARCHAR)
RETURNS VOID AS $$
BEGIN
    DELETE FROM active_viewers
    WHERE link_id = p_link_id AND user_session_id = p_session_id;
END;
$$ LANGUAGE plpgsql;