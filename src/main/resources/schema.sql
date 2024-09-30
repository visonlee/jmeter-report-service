create database jmeter default character set utf8mb4 collate utf8mb4_unicode_ci;


CREATE TABLE `user` (
  `id` bigint auto_increment,
  `username` VARCHAR(64) NOT NULL,
  `email` VARCHAR(255) NOT NULL,
  `password` VARCHAR(256) NOT NULL,
  `country` VARCHAR(4) NOT NULL,
  `create_time` timestamp NOT NULL default current_timestamp,
   `update_time` timestamp NOT NULL default current_timestamp on update current_timestamp,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `username_UNIQUE` (`username` ASC) VISIBLE,
  UNIQUE INDEX `email_UNIQUE` (`email` ASC) VISIBLE
  );

CREATE TABLE `script` (
  `id` bigint auto_increment,
  `user_id` bigint not null,
  `filename` VARCHAR(128) NOT NULL,
  `uploaded_full_path` VARCHAR(512) NOT NULL,
  `extracted_full_path` VARCHAR(512) NOT NULL,
  `report_directory` VARCHAR(512) NOT NULL,
  `status` integer NOT NULL COMMENT "0:not_started 1:waiting 2:running 3:completed 4:cancelled 5:error",
  `start_time` timestamp,
  `end_time` timestamp,
  `create_time` timestamp NOT NULL default current_timestamp,
  `update_time` timestamp NOT NULL default current_timestamp on update current_timestamp,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `filename_UNIQUE` (`filename` ASC) VISIBLE
  );

  CREATE TABLE `script_job` (
    `id` bigint auto_increment,
    `script_id` bigint not null,
    `expected_start_time` timestamp NOT NULL,
    `create_time` timestamp NOT NULL default current_timestamp,
     PRIMARY KEY (`id`),
     UNIQUE (script_id)
    );

  CREATE TABLE `script_run_history` (
    `id` bigint auto_increment,
    `script_id` bigint not null,
    `report_path` VARCHAR(512),
    `start_time` timestamp,
    `end_time` timestamp,
    `create_time` timestamp NOT NULL default current_timestamp,
    `update_time` timestamp NOT NULL default current_timestamp on update current_timestamp,
    PRIMARY KEY (`id`)
    );

  CREATE TABLE `sample_result` (
    `id` bigint auto_increment,
    `run_history_id` bigint not null,
    `timeStamp` bigint,
    `elapsed` bigint,
    `label` VARCHAR(255),
    `response_code` int,
    `response_message` VARCHAR(512),
    `thread_name` VARCHAR(128),
    `data_type` VARCHAR(32),
    `success` VARCHAR(16),
    `failure_message` VARCHAR(512),
    `bytes` bigint,
    `sent_bytes` bigint,
    `grp_threads` int,
    `all_threads` int,
    `url` VARCHAR(1024),
    `latency` bigint,
    `idle_time` bigint,
    `connect` bigint,
    `create_time` timestamp NOT NULL default current_timestamp,
    PRIMARY KEY (`id`)
    );

