-- Resume review queue: filter by status and show the newest submissions first.
ALTER TABLE biz_resume
    ADD INDEX idx_resume_status_update (status, update_time);
