UPDATE message_graph
SET variant = SUBSTRING(variant FROM POSITION('/' IN variant) + 1)
WHERE POSITION('/' IN variant) > 0;
