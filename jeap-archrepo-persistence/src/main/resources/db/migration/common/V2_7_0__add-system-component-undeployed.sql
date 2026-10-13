ALTER TABLE system_component
    ADD COLUMN undeployed    boolean                  NOT NULL DEFAULT false,
    ADD COLUMN undeployed_at timestamp with time zone;
