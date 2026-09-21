CREATE TABLE IF NOT EXISTS event_journal (
  ordering BIGSERIAL,
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  deleted BOOLEAN DEFAULT FALSE NOT NULL,
  writer VARCHAR(255) NOT NULL,
  adapter_manifest VARCHAR(255) NOT NULL,
  event_payload BYTEA NOT NULL,
  event_manifest VARCHAR(255) NOT NULL,
  meta_payload BYTEA,
  meta_manifest VARCHAR(255),
  timestamp BIGINT NOT NULL,
  tags VARCHAR(255),
  PRIMARY KEY(persistence_id, sequence_number)
  );

CREATE UNIQUE INDEX IF NOT EXISTS event_journal_ordering_idx ON event_journal(ordering);

CREATE TABLE IF NOT EXISTS snapshot (
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  created BIGINT NOT NULL,
  snapshot_payload BYTEA NOT NULL,
  snapshot_manifest VARCHAR(255) NOT NULL,
  snapshot_meta BYTEA,
  PRIMARY KEY(persistence_id, sequence_number)
  );
