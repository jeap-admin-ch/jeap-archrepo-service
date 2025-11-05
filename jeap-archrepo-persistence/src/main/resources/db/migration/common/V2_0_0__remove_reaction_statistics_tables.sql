-- Remove reaction statistics related tables
DROP TABLE IF EXISTS reaction_property CASCADE;
DROP TABLE IF EXISTS action CASCADE;

-- After removing dependent tables, remove the reaction_statistics table
DROP TABLE IF EXISTS reaction_statistics CASCADE;
