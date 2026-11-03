-- Spec 0003: evaluation runs. review_mode is HUMAN (the default) or SKIP; SKIP marks an
-- evaluation run whatever its status. A SKIP run that produces a proposal ends in the
-- EVALUATED status (status is free text, so the new value needs no schema change).

ALTER TABLE run ADD COLUMN review_mode text NOT NULL DEFAULT 'HUMAN';

CREATE INDEX run_review_mode_created_at_idx ON run (review_mode, created_at);
