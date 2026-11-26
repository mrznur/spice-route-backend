-- B3.4 — Unique cache key on ai_summaries so concurrent requests
-- for the same branch/date/content never produce duplicate rows.
-- First restaurant was made non-unique in V1 to allow partial saves;
-- we now enforce the full composite key.

CREATE UNIQUE INDEX IF NOT EXISTS idx_ai_summaries_unique_cache
    ON ai_summaries (restaurant_id, branch_id, date_from, date_to, input_hash);
