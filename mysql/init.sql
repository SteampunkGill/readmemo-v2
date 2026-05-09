-- Database: document_management_db

-- Drop tables if they exist, in an order that respects foreign key constraints.
DROP TABLE IF EXISTS `feedback_votes`;
DROP TABLE IF EXISTS `feedback_replies`;
DROP TABLE IF EXISTS `feedback_change_log`;
DROP TABLE IF EXISTS `user_achievements`;
DROP TABLE IF EXISTS `user_feedback`;
DROP TABLE IF EXISTS `notifications`;
DROP TABLE IF EXISTS `notification_settings`;
DROP TABLE IF EXISTS `notification_subscriptions`;
DROP TABLE IF EXISTS `document_ocr_results`;
DROP TABLE IF EXISTS `ocr_tasks`;
DROP TABLE IF EXISTS `ocr_exports`;
DROP TABLE IF EXISTS `document_ocr_settings`;
DROP TABLE IF EXISTS `offline_documents`;
DROP TABLE IF EXISTS `offline_downloads`;
DROP TABLE IF EXISTS `offline_sync_tasks`;
DROP TABLE IF EXISTS `offline_sync_history`;
DROP TABLE IF EXISTS `offline_highlights`;
DROP TABLE IF EXISTS `offline_notes`;
DROP TABLE IF EXISTS `offline_reviews`;
DROP TABLE IF EXISTS `offline_vocabulary`;
DROP TABLE IF EXISTS `offline_settings`;
DROP TABLE IF EXISTS `saved_search_results`;
DROP TABLE IF EXISTS `saved_search_tags`;
DROP TABLE IF EXISTS `search_filters`;
DROP TABLE IF EXISTS `filter_tags`;
DROP TABLE IF EXISTS `user_vocabulary_tags`;
DROP TABLE IF EXISTS `review_items`;
DROP TABLE IF EXISTS `user_vocabulary`;
DROP TABLE IF EXISTS `document_tag_relations`;
DROP TABLE IF EXISTS `note_attachments`;
DROP TABLE IF EXISTS `document_notes`;
DROP TABLE IF EXISTS `document_highlights`;
DROP TABLE IF EXISTS `reading_history`;
DROP TABLE IF EXISTS `document_processing_queue`;
DROP TABLE IF EXISTS `documents`;
DROP TABLE IF EXISTS `folders`;
DROP TABLE IF EXISTS `password_reset_tokens`;
DROP TABLE IF EXISTS `user_sessions`;
DROP TABLE IF EXISTS `user_settings`;
DROP TABLE IF EXISTS `document_pages`;
DROP TABLE IF EXISTS `document_tags`;
DROP TABLE IF EXISTS `search_history`;
DROP TABLE IF EXISTS `voice_search_history`;
DROP TABLE IF EXISTS `daily_learning_stats`;
DROP TABLE IF EXISTS `review_sessions`;
DROP TABLE IF EXISTS `export_templates`;
DROP TABLE IF EXISTS `sync_logs`;
DROP TABLE IF EXISTS `batch_ocr_tasks`;
DROP TABLE IF EXISTS `app_versions`;
DROP TABLE IF EXISTS `faq`;
DROP TABLE IF EXISTS `feedback_messages`;
DROP TABLE IF EXISTS `import_history`;
DROP TABLE IF EXISTS `user_limits`;
DROP TABLE IF EXISTS `word_definitions`;
DROP TABLE IF EXISTS `word_examples`;
DROP TABLE IF EXISTS `word_relations`;
DROP TABLE IF EXISTS `word_lookup_history`;
DROP TABLE IF EXISTS `vocabulary_mastery_stats`;
DROP TABLE IF EXISTS `spaced_repetition_schedule`;
DROP TABLE IF EXISTS `quiz_questions`;
DROP TABLE IF EXISTS `words`;
DROP TABLE IF EXISTS `learning_achievements`;
DROP TABLE IF EXISTS `feedback_categories`;
DROP TABLE IF EXISTS `vocabulary_tags`;
DROP TABLE IF EXISTS `learning_goals`;
DROP TABLE IF EXISTS `third_party_logins`;
DROP TABLE IF EXISTS `review_settings`;
DROP TABLE IF EXISTS `reading_settings`;
DROP TABLE IF EXISTS `document_categories`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `undo_log`;

-- Seata undo_log table
CREATE TABLE IF NOT EXISTS `undo_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `branch_id` BIGINT(20) NOT NULL COMMENT '分支事务ID',
  `xid` VARCHAR(128) NOT NULL COMMENT '全局事务ID',
  `context` VARCHAR(128) NOT NULL COMMENT '上下文信息',
  `rollback_info` LONGBLOB NOT NULL COMMENT '回滚信息',
  `log_status` INT(11) NOT NULL COMMENT '状态: 0-正常, 1-全局事务已提交, 2-全局事务已回滚',
  `log_created` DATETIME(6) NOT NULL COMMENT '创建时间',
  `log_modified` DATETIME(6) NOT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='分布式事务回滚日志表';

-- Table structure for `users`
CREATE TABLE `users` (
  `user_id` INT(11) NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `nickname` VARCHAR(50) DEFAULT NULL,
  `avatar_url` VARCHAR(255) DEFAULT NULL,
  `bio` TEXT,
  `location` VARCHAR(100) DEFAULT NULL,
  `website` VARCHAR(255) DEFAULT NULL,
  `user_type` VARCHAR(50) DEFAULT 'free',
  `role` VARCHAR(20) DEFAULT 'user',
  `is_verified` TINYINT(1) DEFAULT 0,
  `preferences` JSON DEFAULT NULL,
  `privacy_settings` JSON DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_login_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `password_reset_tokens`
CREATE TABLE `password_reset_tokens` (
  `token_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `token` VARCHAR(255) NOT NULL,
  `expires_at` TIMESTAMP NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`token_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `password_reset_tokens_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `user_sessions`
CREATE TABLE `user_sessions` (
  `session_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `access_token` VARCHAR(255) NOT NULL,
  `refresh_token` VARCHAR(255) NOT NULL,
  `expires_at` TIMESTAMP NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`session_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `user_sessions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `user_settings`
CREATE TABLE `user_settings` (
  `setting_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `setting_type` VARCHAR(50) NOT NULL,
  `setting_key` VARCHAR(100) NOT NULL,
  `setting_value` TEXT,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`setting_id`),
  KEY `user_id` (`user_id`),
  KEY `setting_type` (`setting_type`),
  UNIQUE KEY `unique_user_setting` (`user_id`, `setting_type`, `setting_key`),
  CONSTRAINT `user_settings_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `folders`
CREATE TABLE `folders` (
  `folder_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `parent_folder_id` INT(11) DEFAULT NULL,
  `folder_name` VARCHAR(255) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`folder_id`),
  KEY `user_id` (`user_id`),
  KEY `parent_folder_id` (`parent_folder_id`),
  CONSTRAINT `folders_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `folders_ibfk_2` FOREIGN KEY (`parent_folder_id`) REFERENCES `folders` (`folder_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `documents`
CREATE TABLE `documents` (
  `document_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `folder_id` INT(11) DEFAULT NULL,
  `title` VARCHAR(255) NOT NULL,
  `author` VARCHAR(100) DEFAULT NULL,
  `description` TEXT,
  `file_path` VARCHAR(255) NOT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `file_size` BIGINT(20) DEFAULT NULL,
  `file_type` VARCHAR(100) DEFAULT NULL,
  `language` VARCHAR(20) DEFAULT NULL,
  `page_count` INT(11) DEFAULT NULL,
  `reading_progress` DOUBLE DEFAULT 0.00,
  `current_page` INT(11) DEFAULT 1,
  `is_public` TINYINT(1) DEFAULT 0,
  `is_favorite` TINYINT(1) DEFAULT 0,
  `is_processed` TINYINT(1) DEFAULT 0,
  `processing_status` VARCHAR(50) DEFAULT 'pending',
  `processing_progress` INT(11) DEFAULT 0,
  `processing_error` TEXT,
  `status` VARCHAR(50) DEFAULT 'active',
  `share_link` VARCHAR(255) DEFAULT NULL,
  `share_password` VARCHAR(255) DEFAULT NULL,
  `share_expiry_date` TIMESTAMP NULL DEFAULT NULL,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_read_at` TIMESTAMP NULL DEFAULT NULL,
  `processing_started_at` TIMESTAMP NULL DEFAULT NULL,
  `processing_completed_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`document_id`),
  KEY `user_id` (`user_id`),
  KEY `folder_id` (`folder_id`),
  CONSTRAINT `documents_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `documents_ibfk_2` FOREIGN KEY (`folder_id`) REFERENCES `folders` (`folder_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `document_pages`
CREATE TABLE `document_pages` (
  `page_id` VARCHAR(50) NOT NULL,
  `document_id` INT(11) NOT NULL,
  `page_number` INT(11) NOT NULL,
  `content` TEXT,
  `html_content` TEXT,
  `word_count` INT(11) DEFAULT 0,
  `character_count` INT(11) DEFAULT 0,
  `has_images` TINYINT(1) DEFAULT 0,
  `images` JSON DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`page_id`),
  UNIQUE KEY `unique_document_page` (`document_id`, `page_number`),
  KEY `document_id` (`document_id`),
  CONSTRAINT `document_pages_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `document_tags`
CREATE TABLE `document_tags` (
  `tag_id` INT(11) NOT NULL AUTO_INCREMENT,
  `tag_name` VARCHAR(100) NOT NULL UNIQUE,
  `color` VARCHAR(20) DEFAULT NULL,
  `description` TEXT,
  `tag_type` VARCHAR(50) DEFAULT 'document',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `document_tag_relations`
CREATE TABLE `document_tag_relations` (
  `relation_id` INT(11) NOT NULL AUTO_INCREMENT,
  `document_id` INT(11) NOT NULL,
  `tag_id` INT(11) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`relation_id`),
  KEY `document_id` (`document_id`),
  KEY `tag_id` (`tag_id`),
  UNIQUE KEY `unique_document_tag` (`document_id`, `tag_id`),
  CONSTRAINT `document_tag_relations_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`) ON DELETE CASCADE,
  CONSTRAINT `document_tag_relations_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `document_tags` (`tag_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `document_processing_queue`
CREATE TABLE `document_processing_queue` (
  `queue_id` INT(11) NOT NULL AUTO_INCREMENT,
  `document_id` INT(11) NOT NULL,
  `status` VARCHAR(50) NOT NULL DEFAULT 'pending',
  `priority` INT(11) DEFAULT 1,
  `error_message` TEXT,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`queue_id`),
  KEY `document_id` (`document_id`),
  CONSTRAINT `document_processing_queue_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `document_highlights`
CREATE TABLE `document_highlights` (
  `highlight_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `document_id` INT(11) NOT NULL,
  `page` INT(11) NOT NULL,
  `text` TEXT NOT NULL,
  `position` JSON DEFAULT NULL,
  `color` VARCHAR(20) DEFAULT 'yellow',
  `note` TEXT,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`highlight_id`),
  KEY `user_id` (`user_id`),
  KEY `document_id` (`document_id`),
  CONSTRAINT `document_highlights_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `document_highlights_ibfk_2` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `document_notes`
CREATE TABLE `document_notes` (
  `note_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `document_id` INT(11) NOT NULL,
  `page` INT(11) NOT NULL,
  `content` TEXT NOT NULL,
  `position` JSON DEFAULT NULL,
  `highlight_id` INT(11) DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`note_id`),
  KEY `user_id` (`user_id`),
  KEY `document_id` (`document_id`),
  KEY `highlight_id` (`highlight_id`),
  CONSTRAINT `document_notes_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `document_notes_ibfk_2` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`) ON DELETE CASCADE,
  CONSTRAINT `document_notes_ibfk_3` FOREIGN KEY (`highlight_id`) REFERENCES `document_highlights` (`highlight_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `reading_history`
CREATE TABLE `reading_history` (
  `history_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `document_id` INT(11) NOT NULL,
  `page` INT(11) NOT NULL,
  `is_bookmark` TINYINT(1) DEFAULT 0,
  `start_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `end_time` TIMESTAMP NULL DEFAULT NULL,
  `reading_time` INT(11) DEFAULT 0,
  `pages_read` INT(11) DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`history_id`),
  KEY `user_id` (`user_id`),
  KEY `document_id` (`document_id`),
  CONSTRAINT `reading_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `reading_history_ibfk_2` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `sync_logs`
CREATE TABLE `sync_logs` (
  `log_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `operation_type` VARCHAR(50) NOT NULL,
  `entity_type` VARCHAR(50) NOT NULL,
  `entity_ids` TEXT,
  `status` VARCHAR(50) NOT NULL,
  `details` JSON DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `sync_logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `export_templates`
CREATE TABLE `export_templates` (
  `template_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `template_name` VARCHAR(100) NOT NULL,
  `template_type` VARCHAR(50) NOT NULL,
  `export_format` VARCHAR(20) NOT NULL,
  `description` TEXT,
  `config` JSON DEFAULT NULL,
  `is_default` TINYINT(1) DEFAULT 0,
  `is_public` TINYINT(1) DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`template_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `export_templates_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `words`
CREATE TABLE `words` (
  `word_id` INT(11) NOT NULL AUTO_INCREMENT,
  `word` VARCHAR(100) NOT NULL,
  `language` VARCHAR(20) NOT NULL DEFAULT 'en',
  `phonetic` VARCHAR(255) DEFAULT NULL,
  `translation_pos` TEXT,
  `collins` TEXT,
  `oxford` TEXT,
  `tag` TEXT,
  `bnc` TEXT,
  `frq` INT DEFAULT NULL,
  `exchange` TEXT,
  `detail` TEXT,
  `audio` VARCHAR(255) DEFAULT NULL,
  `metric` INT DEFAULT NULL,
  `part_of_speech` VARCHAR(50) DEFAULT NULL,
  `difficulty` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`word_id`),
  UNIQUE KEY `word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `word_definitions`
CREATE TABLE `word_definitions` (
  `definition_id` INT(11) NOT NULL AUTO_INCREMENT,
  `word_id` INT(11) NOT NULL,
  `definition` TEXT NOT NULL,
  `order_index` INT(11) DEFAULT 0,
  PRIMARY KEY (`definition_id`),
  KEY `word_id` (`word_id`),
  CONSTRAINT `word_definitions_ibfk_1` FOREIGN KEY (`word_id`) REFERENCES `words` (`word_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `word_examples`
CREATE TABLE `word_examples` (
  `example_id` INT(11) NOT NULL AUTO_INCREMENT,
  `word_id` INT(11) NOT NULL,
  `example_sentence` TEXT NOT NULL,
  `translation` TEXT,
  `source` VARCHAR(255) DEFAULT NULL,
  `order_index` INT(11) DEFAULT 0,
  PRIMARY KEY (`example_id`),
  KEY `word_id` (`word_id`),
  CONSTRAINT `word_examples_ibfk_1` FOREIGN KEY (`word_id`) REFERENCES `words` (`word_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `word_relations`
CREATE TABLE `word_relations` (
  `relation_id` INT(11) NOT NULL AUTO_INCREMENT,
  `word_id` INT(11) NOT NULL,
  `related_word_id` INT(11) NOT NULL,
  `relationship_type` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`relation_id`),
  KEY `word_id` (`word_id`),
  KEY `related_word_id` (`related_word_id`),
  CONSTRAINT `word_relations_ibfk_1` FOREIGN KEY (`word_id`) REFERENCES `words` (`word_id`) ON DELETE CASCADE,
  CONSTRAINT `word_relations_ibfk_2` FOREIGN KEY (`related_word_id`) REFERENCES `words` (`word_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `user_vocabulary`
CREATE TABLE `user_vocabulary` (
  `user_vocab_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `word_id` INT(11) NOT NULL,
  `word` VARCHAR(200) NOT NULL,
  `language` VARCHAR(20) NOT NULL DEFAULT 'en',
  `phonetic` VARCHAR(255) DEFAULT NULL,
  `definition` TEXT,
  `translation_pos` TEXT,
  `collins` TEXT,
  `oxford` TEXT,
  `tag` TEXT,
  `bnc` TEXT,
  `frq` INT DEFAULT NULL,
  `exchange` TEXT,
  `detail` TEXT,
  `audio` VARCHAR(255) DEFAULT NULL,
  `metric` INT DEFAULT NULL,
  `example` TEXT,
  `notes` TEXT,
  `status` VARCHAR(50) DEFAULT 'new',
  `mastery_level` INT(11) DEFAULT 0,
  `review_count` INT(11) DEFAULT 0,
  `last_reviewed_at` TIMESTAMP NULL DEFAULT NULL,
  `next_review_at` TIMESTAMP NULL DEFAULT NULL,
  `source` VARCHAR(255) DEFAULT NULL,
  `source_page` INT(11) DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_vocab_id`),
  KEY `user_id` (`user_id`),
  KEY `word_id` (`word_id`),
  KEY `status` (`status`),
  KEY `language` (`language`),
  UNIQUE KEY `unique_user_word` (`user_id`, `word`, `language`),
  CONSTRAINT `user_vocabulary_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `user_vocabulary_ibfk_2` FOREIGN KEY (`word_id`) REFERENCES `words` (`word_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `vocabulary_tags`
CREATE TABLE `vocabulary_tags` (
  `tag_id` INT(11) NOT NULL AUTO_INCREMENT,
  `tag_name` VARCHAR(100) NOT NULL UNIQUE,
  `color` VARCHAR(20) DEFAULT NULL,
  `description` TEXT,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `user_vocabulary_tags`
CREATE TABLE `user_vocabulary_tags` (
  `user_vocab_tag_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_vocab_id` INT(11) NOT NULL,
  `tag_id` INT(11) NOT NULL,
  PRIMARY KEY (`user_vocab_tag_id`),
  KEY `user_vocab_id` (`user_vocab_id`),
  KEY `tag_id` (`tag_id`),
  UNIQUE KEY `unique_user_vocab_tag` (`user_vocab_id`, `tag_id`),
  CONSTRAINT `user_vocabulary_tags_ibfk_1` FOREIGN KEY (`user_vocab_id`) REFERENCES `user_vocabulary` (`user_vocab_id`) ON DELETE CASCADE,
  CONSTRAINT `user_vocabulary_tags_ibfk_2` FOREIGN KEY (`tag_id`) REFERENCES `vocabulary_tags` (`tag_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `review_sessions`
CREATE TABLE `review_sessions` (
  `session_id` VARCHAR(50) NOT NULL,
  `user_id` INT(11) NOT NULL,
  `mode` VARCHAR(50) NOT NULL,
  `total_words` INT(11) DEFAULT 0,
  `correct_words` INT(11) DEFAULT 0,
  `accuracy` DOUBLE DEFAULT 0.00,
  `duration` INT(11) DEFAULT 0,
  `language` VARCHAR(20) DEFAULT NULL,
  `status` VARCHAR(50) DEFAULT 'pending',
  `completed_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`session_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `review_sessions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `review_items`
CREATE TABLE `review_items` (
  `review_item_id` INT(11) NOT NULL AUTO_INCREMENT,
  `session_id` VARCHAR(50) NOT NULL,
  `user_vocab_id` INT(11) NOT NULL,
  `word_id` INT(11) NOT NULL,
  `user_answer` TEXT,
  `is_correct` TINYINT(1) NOT NULL,
  `response_time` INT(11) DEFAULT NULL,
  `review_type` VARCHAR(50) DEFAULT NULL,
  `mastery_level_before` INT(11) DEFAULT NULL,
  `mastery_level_after` INT(11) DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`review_item_id`),
  KEY `session_id` (`session_id`),
  KEY `user_vocab_id` (`user_vocab_id`),
  KEY `word_id` (`word_id`),
  CONSTRAINT `review_items_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `review_sessions` (`session_id`) ON DELETE CASCADE,
  CONSTRAINT `review_items_ibfk_2` FOREIGN KEY (`user_vocab_id`) REFERENCES `user_vocabulary` (`user_vocab_id`) ON DELETE CASCADE,
  CONSTRAINT `review_items_ibfk_3` FOREIGN KEY (`word_id`) REFERENCES `words` (`word_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `daily_learning_stats`
CREATE TABLE `daily_learning_stats` (
  `stat_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `date` DATE NOT NULL,
  `documents_read` INT(11) DEFAULT 0,
  `pages_read` INT(11) DEFAULT 0,
  `streak_days` INT(11) DEFAULT 0,
  `words_reviewed` INT(11) DEFAULT 0,
  `words_correct` INT(11) DEFAULT 0,
  `words_incorrect` INT(11) DEFAULT 0,
  `reading_time` INT(11) DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`stat_id`),
  UNIQUE KEY `unique_user_date` (`user_id`, `date`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `daily_learning_stats_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `learning_achievements`
CREATE TABLE `learning_achievements` (
  `achievement_id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL UNIQUE,
  `description` TEXT,
  `icon_url` VARCHAR(255) DEFAULT NULL,
  `category` VARCHAR(50) DEFAULT NULL,
  `points` INT(11) DEFAULT 0,
  `total_required` INT(11) DEFAULT 0,
  `rarity` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`achievement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `user_achievements`
CREATE TABLE `user_achievements` (
  `user_achievement_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `achievement_id` INT(11) NOT NULL,
  `unlocked_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_achievement_id`),
  KEY `user_id` (`user_id`),
  KEY `achievement_id` (`achievement_id`),
  UNIQUE KEY `unique_user_achievement` (`user_id`, `achievement_id`),
  CONSTRAINT `user_achievements_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `user_achievements_ibfk_2` FOREIGN KEY (`achievement_id`) REFERENCES `learning_achievements` (`achievement_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `feedback_categories`
CREATE TABLE `feedback_categories` (
  `category_id` VARCHAR(50) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `value` VARCHAR(100) NOT NULL UNIQUE,
  `description` TEXT,
  `icon` VARCHAR(50) DEFAULT NULL,
  `color` VARCHAR(20) DEFAULT NULL,
  `order_index` INT(11) DEFAULT 0,
  `is_active` TINYINT(1) DEFAULT 1,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `user_feedback`
CREATE TABLE `user_feedback` (
  `feedback_id` VARCHAR(50) NOT NULL,
  `user_id` INT(11) NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `content` TEXT NOT NULL,
  `type` VARCHAR(50) DEFAULT 'other',
  `status` VARCHAR(50) DEFAULT 'pending',
  `priority` VARCHAR(20) DEFAULT 'medium',
  `upvotes` INT(11) DEFAULT 0,
  `downvotes` INT(11) DEFAULT 0,
  `view_count` INT(11) DEFAULT 0,
  `comment_count` INT(11) DEFAULT 0,
  `attachments` JSON DEFAULT NULL,
  `metadata` JSON DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `assigned_to` VARCHAR(50) NULL,
  `assigned_at` TIMESTAMP NULL DEFAULT NULL,
  `completed_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`feedback_id`),
  KEY `user_id` (`user_id`),
  INDEX `idx_feedback_type` (`type`),
  INDEX `idx_feedback_status` (`status`),
  INDEX `idx_feedback_priority` (`priority`),
  INDEX `idx_feedback_created_at` (`created_at`),
  CONSTRAINT `user_feedback_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `feedback_votes`
CREATE TABLE `feedback_votes` (
  `vote_id` INT(11) NOT NULL AUTO_INCREMENT,
  `feedback_id` VARCHAR(50) NOT NULL,
  `user_id` INT(11) NOT NULL,
  `vote_type` ENUM('upvote','downvote') NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`vote_id`),
  KEY `feedback_id` (`feedback_id`),
  KEY `user_id` (`user_id`),
  UNIQUE KEY `unique_user_feedback_vote` (`feedback_id`, `user_id`, `vote_type`),
  CONSTRAINT `feedback_votes_ibfk_1` FOREIGN KEY (`feedback_id`) REFERENCES `user_feedback` (`feedback_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `feedback_replies`
CREATE TABLE `feedback_replies` (
  `reply_id` VARCHAR(50) NOT NULL,
  `feedback_id` VARCHAR(50) NOT NULL,
  `user_id` INT(11) NOT NULL,
  `message` TEXT NOT NULL,
  `is_internal` TINYINT(1) DEFAULT 0,
  `attachments` JSON DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`reply_id`),
  KEY `feedback_id` (`feedback_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `feedback_replies_ibfk_1` FOREIGN KEY (`feedback_id`) REFERENCES `user_feedback` (`feedback_id`) ON DELETE CASCADE,
  CONSTRAINT `feedback_replies_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `feedback_change_log`
CREATE TABLE `feedback_change_log` (
  `log_id` INT(11) NOT NULL AUTO_INCREMENT,
  `feedback_id` VARCHAR(50) NOT NULL,
  `field` VARCHAR(50) NOT NULL,
  `old_value` TEXT,
  `new_value` TEXT,
  `changed_by` INT(11) NOT NULL,
  `reason` VARCHAR(255) DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `feedback_id` (`feedback_id`),
  KEY `changed_by` (`changed_by`),
  CONSTRAINT `feedback_change_log_ibfk_1` FOREIGN KEY (`feedback_id`) REFERENCES `user_feedback` (`feedback_id`) ON DELETE CASCADE,
  CONSTRAINT `feedback_change_log_ibfk_2` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `notifications`
CREATE TABLE `notifications` (
  `notification_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `type` VARCHAR(50) NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `message` TEXT NOT NULL,
  `icon_url` VARCHAR(255) DEFAULT NULL,
  `image_url` VARCHAR(255) DEFAULT NULL,
  `action_url` VARCHAR(255) DEFAULT NULL,
  `target_type` VARCHAR(50) DEFAULT NULL,
  `target_id` VARCHAR(50) DEFAULT NULL,
  `metadata` JSON DEFAULT NULL,
  `is_read` TINYINT(1) DEFAULT 0,
  `read_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`notification_id`),
  KEY `user_id` (`user_id`),
  INDEX `idx_notification_user_id` (`user_id`),
  INDEX `idx_notification_created_at` (`created_at`),
  CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `notification_settings`
CREATE TABLE `notification_settings` (
  `setting_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL UNIQUE,
  `email_notifications` TINYINT(1) DEFAULT 1,
  `push_notifications` TINYINT(1) DEFAULT 1,
  `desktop_notifications` TINYINT(1) DEFAULT 1,
  `notification_frequency` VARCHAR(50) DEFAULT 'immediate',
  `quiet_hours_enabled` TINYINT(1) DEFAULT 1,
  `quiet_hours_start` TIME DEFAULT '22:00:00',
  `quiet_hours_end` TIME DEFAULT '08:00:00',
  `notification_types` JSON DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`setting_id`),
  CONSTRAINT `notification_settings_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `notification_subscriptions`
CREATE TABLE `notification_subscriptions` (
  `subscription_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `channel` VARCHAR(50) NOT NULL,
  `device_token` VARCHAR(255) DEFAULT NULL,
  `is_active` TINYINT(1) DEFAULT 1,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`subscription_id`),
  KEY `user_id` (`user_id`),
  UNIQUE KEY `unique_user_channel` (`user_id`, `channel`),
  CONSTRAINT `notification_subscriptions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `document_ocr_results`
CREATE TABLE `document_ocr_results` (
  `ocr_id` VARCHAR(50) NOT NULL,
  `document_id` INT(11) NOT NULL,
  `page_number` INT(11) NOT NULL,
  `ocr_text` TEXT,
  `confidence` DOUBLE DEFAULT NULL,
  `words_json` TEXT,
  `lines_json` TEXT,
  `blocks_json` TEXT,
  `metadata_json` TEXT,
  `created_at` TIMESTAMP NULL DEFAULT NULL,
  `updated_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`ocr_id`),
  KEY `document_id` (`document_id`),
  CONSTRAINT `document_ocr_results_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `batch_ocr_tasks`
CREATE TABLE `batch_ocr_tasks` (
  `batch_id` VARCHAR(50) NOT NULL,
  `total_documents` INT(11) NOT NULL,
  `total_pages` INT(11) NOT NULL,
  `processed_documents` INT(11) DEFAULT 0,
  `processed_pages` INT(11) DEFAULT 0,
  `successful_pages` INT(11) DEFAULT 0,
  `failed_pages` INT(11) DEFAULT 0,
  `average_confidence` DECIMAL(5,2) DEFAULT 0.00,
  `status` VARCHAR(20) NOT NULL,
  `options_json` TEXT,
  `results_json` TEXT,
  `error_message` TEXT,
  `estimated_time` INT(11) DEFAULT NULL,
  `started_at` TIMESTAMP NULL DEFAULT NULL,
  `completed_at` TIMESTAMP NULL DEFAULT NULL,
  `cancelled_at` TIMESTAMP NULL DEFAULT NULL,
  `cancel_reason` TEXT NULL,
  `created_at` TIMESTAMP NULL DEFAULT NULL,
  `updated_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `ocr_tasks`
CREATE TABLE `ocr_tasks` (
  `task_id` VARCHAR(50) NOT NULL,
  `document_id` INT(11) NOT NULL,
  `page_number` INT(11) DEFAULT NULL,
  `content_type` VARCHAR(20) DEFAULT NULL,
  `content_length` INT(11) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL,
  `progress` INT(11) DEFAULT 0,
  `options_json` TEXT,
  `result_json` TEXT,
  `error_message` TEXT,
  `estimated_time` INT(11) DEFAULT NULL,
  `started_at` TIMESTAMP NULL DEFAULT NULL,
  `completed_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NULL DEFAULT NULL,
  `updated_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`task_id`),
  KEY `document_id` (`document_id`),
  CONSTRAINT `ocr_tasks_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `ocr_exports`
CREATE TABLE `ocr_exports` (
  `export_id` VARCHAR(50) NOT NULL,
  `document_id` INT(11) NOT NULL,
  `format` VARCHAR(10) NOT NULL,
  `total_pages` INT(11) NOT NULL,
  `file_path` VARCHAR(500) DEFAULT NULL,
  `file_size` BIGINT(20) DEFAULT NULL,
  `download_url` VARCHAR(500) DEFAULT NULL,
  `created_at` TIMESTAMP NULL DEFAULT NULL,
  `expires_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`export_id`),
  KEY `document_id` (`document_id`),
  CONSTRAINT `ocr_exports_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `document_ocr_settings`
CREATE TABLE `document_ocr_settings` (
  `setting_id` VARCHAR(50) NOT NULL,
  `document_id` INT(11) NOT NULL,
  `default_language` VARCHAR(20) DEFAULT NULL,
  `default_confidence` INT(11) DEFAULT NULL,
  `auto_preprocess` TINYINT(1) DEFAULT NULL,
  `supported_languages` TEXT,
  `engine` VARCHAR(50) DEFAULT NULL,
  `created_at` TIMESTAMP NULL DEFAULT NULL,
  `updated_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`setting_id`),
  KEY `document_id` (`document_id`),
  CONSTRAINT `document_ocr_settings_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `offline_documents`
CREATE TABLE `offline_documents` (
  `offline_doc_id` VARCHAR(50) NOT NULL,
  `document_id` INT(11) NOT NULL,
  `user_id` INT(11) NOT NULL,
  `title` VARCHAR(500) NOT NULL,
  `content` LONGTEXT,
  `description` TEXT,
  `tags` JSON DEFAULT NULL,
  `file_size` BIGINT(20) DEFAULT 0,
  `file_type` VARCHAR(50) DEFAULT NULL,
  `download_url` VARCHAR(500) DEFAULT NULL,
  `is_synced` TINYINT(1) DEFAULT 0,
  `offline_version` INT(11) DEFAULT 1,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`offline_doc_id`),
  KEY `user_id` (`user_id`),
  KEY `document_id` (`document_id`),
  CONSTRAINT `offline_documents_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `offline_documents_ibfk_2` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `offline_downloads`
CREATE TABLE `offline_downloads` (
  `download_id` VARCHAR(50) NOT NULL,
  `user_id` INT(11) NOT NULL,
  `document_id` INT(11) NOT NULL,
  `offline_doc_id` VARCHAR(50) DEFAULT NULL,
  `status` VARCHAR(20) DEFAULT 'pending',
  `progress` INT(11) DEFAULT 0,
  `file_size` BIGINT(20) DEFAULT 0,
  `downloaded_size` BIGINT(20) DEFAULT 0,
  `speed` VARCHAR(50) DEFAULT NULL,
  `eta` VARCHAR(50) DEFAULT NULL,
  `error` TEXT,
  `start_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `end_time` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `batch_id` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`download_id`),
  KEY `user_id` (`user_id`),
  KEY `document_id` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `offline_sync_tasks`
CREATE TABLE `offline_sync_tasks` (
  `task_id` VARCHAR(50) NOT NULL,
  `user_id` INT(11) NOT NULL,
  `task_type` VARCHAR(50) NOT NULL,
  `status` VARCHAR(20) DEFAULT 'running',
  `progress` INT(11) DEFAULT 0,
  `total_operations` INT(11) DEFAULT 0,
  `completed_operations` INT(11) DEFAULT 0,
  `failed_operations` INT(11) DEFAULT 0,
  `estimated_end_time` TIMESTAMP NULL DEFAULT NULL,
  `start_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `end_time` TIMESTAMP NULL DEFAULT NULL,
  `error` TEXT,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `offline_sync_tasks_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `offline_sync_history`
CREATE TABLE `offline_sync_history` (
  `sync_hist_id` VARCHAR(50) NOT NULL,
  `user_id` INT(11) NOT NULL,
  `task_id` VARCHAR(50) NOT NULL,
  `task_type` VARCHAR(50) NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `synced_items` INT(11) DEFAULT 0,
  `failed_items` INT(11) DEFAULT 0,
  `duration` DOUBLE DEFAULT 0.00,
  `error` TEXT,
  `start_time` TIMESTAMP NULL DEFAULT NULL,
  `end_time` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`sync_hist_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `offline_sync_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `offline_highlights`
CREATE TABLE `offline_highlights` (
  `offline_highlight_id` VARCHAR(50) NOT NULL,
  `highlight_id` INT(11) NOT NULL,
  `user_id` INT(11) NOT NULL,
  `document_id` INT(11) NOT NULL,
  `text` TEXT,
  `color` VARCHAR(20) DEFAULT NULL,
  `is_synced` TINYINT(1) DEFAULT 0,
  `offline_version` INT(11) DEFAULT 1,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`offline_highlight_id`),
  KEY `user_id` (`user_id`),
  KEY `document_id` (`document_id`),
  CONSTRAINT `offline_highlights_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `offline_highlights_ibfk_2` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `offline_notes`
CREATE TABLE `offline_notes` (
  `offline_note_id` VARCHAR(50) NOT NULL,
  `note_id` INT(11) NOT NULL,
  `user_id` INT(11) NOT NULL,
  `document_id` INT(11) NOT NULL,
  `content` TEXT,
  `is_synced` TINYINT(1) DEFAULT 0,
  `offline_version` INT(11) DEFAULT 1,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`offline_note_id`),
  KEY `user_id` (`user_id`),
  KEY `document_id` (`document_id`),
  CONSTRAINT `offline_notes_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `offline_notes_ibfk_2` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `offline_reviews`
CREATE TABLE `offline_reviews` (
  `offline_rev_id` VARCHAR(50) NOT NULL,
  `review_id` VARCHAR(50) NOT NULL,
  `user_id` INT(11) NOT NULL,
  `vocabulary_id` INT(11) NOT NULL,
  `difficulty` INT(11) DEFAULT 3,
  `review_time` TIMESTAMP NULL DEFAULT NULL,
  `is_synced` TINYINT(1) DEFAULT 0,
  `offline_version` INT(11) DEFAULT 1,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`offline_rev_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `offline_reviews_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `offline_vocabulary`
CREATE TABLE `offline_vocabulary` (
  `offline_voc_id` VARCHAR(50) NOT NULL,
  `vocabulary_id` INT(11) NOT NULL,
  `user_id` INT(11) NOT NULL,
  `word` VARCHAR(200) NOT NULL,
  `translation` TEXT,
  `is_synced` TINYINT(1) DEFAULT 0,
  `offline_version` INT(11) DEFAULT 1,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`offline_voc_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `offline_vocabulary_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `offline_settings`
CREATE TABLE `offline_settings` (
  `user_id` INT(11) NOT NULL,
  `setting_key` VARCHAR(50) NOT NULL,
  `setting_value` TEXT,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `setting_key`),
  CONSTRAINT `offline_settings_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `search_history`
CREATE TABLE `search_history` (
  `search_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `keyword` VARCHAR(255) NOT NULL,
  `search_type` VARCHAR(50) NOT NULL,
  `result_count` INT(11) DEFAULT 0,
  `timestamp` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`search_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `search_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `saved_search_results`
CREATE TABLE `saved_search_results` (
  `saved_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `search_id` BIGINT(20) NOT NULL,
  `saved_data` JSON NOT NULL,
  `note` TEXT,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`saved_id`),
  KEY `user_id` (`user_id`),
  KEY `search_id` (`search_id`),
  CONSTRAINT `saved_search_results_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `saved_search_results_ibfk_2` FOREIGN KEY (`search_id`) REFERENCES `search_history` (`search_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `saved_search_tags`
CREATE TABLE `saved_search_tags` (
  `saved_search_tag_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `saved_id` BIGINT(20) NOT NULL,
  `tag_name` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`saved_search_tag_id`),
  KEY `saved_id` (`saved_id`),
  UNIQUE KEY `unique_saved_tag` (`saved_id`, `tag_name`),
  CONSTRAINT `saved_search_tags_ibfk_1` FOREIGN KEY (`saved_id`) REFERENCES `saved_search_results` (`saved_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `search_filters`
CREATE TABLE `search_filters` (
  `filter_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` TEXT,
  `filter_type` VARCHAR(50) NOT NULL,
  `filter_data` JSON NOT NULL,
  `is_public` TINYINT(1) DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`filter_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `search_filters_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `filter_tags`
CREATE TABLE `filter_tags` (
  `filter_tag_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `filter_id` BIGINT(20) NOT NULL,
  `tag_name` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`filter_tag_id`),
  KEY `filter_id` (`filter_id`),
  UNIQUE KEY `unique_filter_tag` (`filter_id`, `tag_name`),
  CONSTRAINT `filter_tags_ibfk_1` FOREIGN KEY (`filter_id`) REFERENCES `search_filters` (`filter_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `app_versions`
CREATE TABLE `app_versions` (
  `version_id` INT(11) NOT NULL AUTO_INCREMENT,
  `version_number` VARCHAR(50) NOT NULL,
  `release_date` DATETIME NOT NULL,
  `release_notes` TEXT,
  `is_forced_update` TINYINT(1) DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`version_id`),
  UNIQUE KEY `version_number` (`version_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `faq`
CREATE TABLE `faq` (
  `faq_id` INT(11) NOT NULL AUTO_INCREMENT,
  `question` TEXT NOT NULL,
  `answer` TEXT NOT NULL,
  `category` VARCHAR(100) DEFAULT NULL,
  `tags` JSON DEFAULT NULL,
  `order_index` INT(11) DEFAULT 0,
  `is_active` TINYINT(1) DEFAULT 1,
  `view_count` INT(11) DEFAULT 0,
  `helpful_count` INT(11) DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`faq_id`),
  KEY `category` (`category`),
  KEY `order_index` (`order_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `feedback_messages`
CREATE TABLE `feedback_messages` (
  `message_id` VARCHAR(50) NOT NULL,
  `user_id` INT(11) NULL,
  `title` VARCHAR(255) NOT NULL,
  `description` TEXT NOT NULL,
  `steps` TEXT,
  `expected_result` TEXT,
  `actual_result` TEXT,
  `environment_info` TEXT,
  `screenshot_data` TEXT,
  `priority` VARCHAR(20) DEFAULT 'medium',
  `category` VARCHAR(50) DEFAULT 'general',
  `tags` TEXT,
  `status` VARCHAR(50) DEFAULT 'pending',
  `assigned_to` VARCHAR(50) NULL,
  `assigned_at` TIMESTAMP NULL DEFAULT NULL,
  `resolved_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `import_history`
CREATE TABLE `import_history` (
  `import_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `file_size` BIGINT(20) DEFAULT 0,
  `imported_count` INT(11) DEFAULT 0,
  `import_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`import_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `import_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `user_limits`
CREATE TABLE `user_limits` (
  `limit_id` INT(11) NOT NULL AUTO_INCREMENT,
  `user_type` VARCHAR(50) NOT NULL,
  `limit_type` VARCHAR(100) NOT NULL,
  `limit_value` INT(11) NOT NULL,
  `description` TEXT,
  PRIMARY KEY (`limit_id`),
  UNIQUE KEY `unique_user_limit` (`user_type`, `limit_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `voice_search_history`
CREATE TABLE `voice_search_history` (
  `voice_history_id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `user_id` INT(11) NOT NULL,
  `recognized_text` VARCHAR(255) NOT NULL,
  `language` VARCHAR(50) DEFAULT NULL,
  `result_count` INT(11) DEFAULT 0,
  `timestamp` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`voice_history_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `voice_search_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `learning_goals`
CREATE TABLE `learning_goals` (
  `goal_id` INT PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `type` VARCHAR(50) NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT,
  `target_value` INT NOT NULL,
  `current_value` INT DEFAULT 0,
  `unit` VARCHAR(50),
  `start_date` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `end_date` DATETIME,
  `status` VARCHAR(20) DEFAULT 'active',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `learning_goals_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `third_party_logins`
CREATE TABLE `third_party_logins` (
  `login_id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `provider` VARCHAR(50) NOT NULL,
  `provider_user_id` VARCHAR(255) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `third_party_logins_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  UNIQUE KEY `unique_provider_user` (`provider`, `provider_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `vocabulary_mastery_stats`
CREATE TABLE `vocabulary_mastery_stats` (
  `stat_id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `word_id` INT NOT NULL,
  `mastery_level` INT DEFAULT 0,
  `review_count` INT DEFAULT 0,
  `last_reviewed_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `vocabulary_mastery_stats_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `vocabulary_mastery_stats_ibfk_2` FOREIGN KEY (`word_id`) REFERENCES `words` (`word_id`) ON DELETE CASCADE,
  UNIQUE KEY `unique_user_word_stat` (`user_id`, `word_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `spaced_repetition_schedule`
CREATE TABLE `spaced_repetition_schedule` (
  `schedule_id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `user_vocab_id` INT NOT NULL,
  `next_review_date` TIMESTAMP NULL DEFAULT NULL,
  `interval_days` INT DEFAULT 1,
  `ease_factor` DECIMAL(3,2) DEFAULT 2.5,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `spaced_repetition_schedule_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `spaced_repetition_schedule_ibfk_2` FOREIGN KEY (`user_vocab_id`) REFERENCES `user_vocabulary` (`user_vocab_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `quiz_questions`
CREATE TABLE `quiz_questions` (
  `question_id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `question_text` TEXT NOT NULL,
  `options` JSON,
  `correct_answer` VARCHAR(255) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `quiz_questions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `note_attachments`
CREATE TABLE `note_attachments` (
  `attachment_id` INT AUTO_INCREMENT PRIMARY KEY,
  `note_id` INT(11) NOT NULL,
  `user_id` INT(11) NOT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `file_path` VARCHAR(500) NOT NULL,
  `file_type` VARCHAR(50),
  `file_size` BIGINT,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `note_attachments_ibfk_1` FOREIGN KEY (`note_id`) REFERENCES `document_notes` (`note_id`) ON DELETE CASCADE,
  CONSTRAINT `note_attachments_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `review_settings`
CREATE TABLE `review_settings` (
  `setting_id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL UNIQUE,
  `review_mode` VARCHAR(50) DEFAULT 'spaced',
  `custom_days` INT DEFAULT 7,
  `review_difficulty` VARCHAR(50) DEFAULT 'normal',
  `review_frequency` VARCHAR(50) DEFAULT 'daily',
  `cards_per_session` INT DEFAULT 20,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `review_settings_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `reading_settings`
CREATE TABLE `reading_settings` (
  `setting_id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL UNIQUE,
  `font_size` INT DEFAULT 16,
  `theme` VARCHAR(50) DEFAULT 'light',
  `line_height` DECIMAL(3,1) DEFAULT 1.6,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `reading_settings_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `document_categories`
CREATE TABLE `document_categories` (
  `category_id` INT AUTO_INCREMENT PRIMARY KEY,
  `document_id` INT NOT NULL,
  `category_name` VARCHAR(100) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `document_categories_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `documents` (`document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table structure for `word_lookup_history`
CREATE TABLE `word_lookup_history` (
  `lookup_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `word_id` INT NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `word_lookup_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `word_lookup_history_ibfk_2` FOREIGN KEY (`word_id`) REFERENCES `words` (`word_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_reading_history_user_id ON reading_history(user_id);
CREATE INDEX idx_user_sessions_token ON user_sessions(access_token);
CREATE INDEX idx_user_vocabulary_user_id ON user_vocabulary (user_id);
CREATE INDEX idx_user_vocabulary_language ON user_vocabulary (language);
CREATE INDEX idx_user_vocabulary_status ON user_vocabulary (status);
CREATE INDEX idx_user_vocabulary_mastery_level ON user_vocabulary (mastery_level);
CREATE INDEX idx_user_vocabulary_next_review_at ON user_vocabulary (next_review_at);
CREATE INDEX idx_user_vocabulary_created_at ON user_vocabulary(created_at);
CREATE INDEX idx_user_vocabulary_tags_user_vocab_id ON user_vocabulary_tags (user_vocab_id);
CREATE INDEX idx_user_vocabulary_tags_tag_id ON user_vocabulary_tags (tag_id);
CREATE INDEX idx_vocabulary_tags_tag_name ON vocabulary_tags (tag_name);
CREATE INDEX idx_review_items_session_id ON review_items (session_id);
CREATE INDEX idx_review_items_user_vocab_id ON review_items (user_vocab_id);
CREATE INDEX idx_review_items_word_id ON review_items (word_id);
CREATE INDEX idx_daily_learning_stats_user_id ON daily_learning_stats (user_id);
CREATE INDEX idx_daily_learning_stats_learning_date ON daily_learning_stats (date);
CREATE INDEX idx_user_achievements_user_id ON user_achievements (user_id);
CREATE INDEX idx_user_achievements_achievement_id ON user_achievements (achievement_id);