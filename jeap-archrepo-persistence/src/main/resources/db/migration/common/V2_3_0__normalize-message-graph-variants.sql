UPDATE message_graph
SET variant = ''
WHERE lower(trim(variant)) = 'default'
   OR lower(trim(variant)) = lower(trim(message_type_name))
   OR lower(trim(variant)) = lower(trim(message_type_name)) || '/default';

DELETE FROM message_graph
WHERE id IN (
    SELECT id
    FROM (
        SELECT id,
               row_number() OVER (
                   PARTITION BY message_type_name, variant
                   ORDER BY coalesce(modified_at, created_at) DESC, created_at DESC, id
               ) AS duplicate_number
        FROM message_graph
    ) ranked_graphs
    WHERE duplicate_number > 1
);

CREATE UNIQUE INDEX message_graph_message_type_variant_uq
    ON message_graph (message_type_name, variant);
