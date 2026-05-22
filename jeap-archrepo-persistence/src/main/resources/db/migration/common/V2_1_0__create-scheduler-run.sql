create table scheduler_run
(
    job_name    varchar   not null primary key,
    last_run_at timestamp not null
);
