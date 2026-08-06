UPDATE system_component
SET type = 'GATEWAY'
WHERE type = 'BACKEND_SERVICE'
  AND name LIKE '%-gateway';
