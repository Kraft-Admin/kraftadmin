-- 1. HTTP Client Events
CREATE TABLE kraft_http_client_events (
                                          id TEXT PRIMARY KEY,
                                          trace_id TEXT NOT NULL,
                                          host TEXT,
                                          url TEXT,
                                          method TEXT,
                                          status_code INTEGER,
                                          duration_ms INTEGER,
                                          response_body_size INTEGER DEFAULT 0,
                                          connection_timeout_ms INTEGER,
                                          error_message TEXT,
                                          created_at INTEGER,
                                          payload TEXT,
                                          synced INTEGER DEFAULT 0
);

-- 2. Tasks
CREATE TABLE kraft_tasks (
                             id TEXT PRIMARY KEY,
                             trace_id TEXT NOT NULL,
                             name TEXT,
                             type TEXT,
                             status TEXT,
                             duration_ms INTEGER,
                             error_message TEXT,
                             resource_usage TEXT, -- Serialized JSON
                             node_identifier TEXT,
                             retry_count INTEGER DEFAULT 0,
                             trigger_source TEXT,
                             task_metadata TEXT, -- Serialized JSON
                             created_at INTEGER,
                             payload TEXT,
                             synced INTEGER DEFAULT 0
);

-- 3. Telemetry (Canonical)
CREATE TABLE kraft_telemetry (
                                 id TEXT PRIMARY KEY,
                                 trace_id TEXT,
                                 type TEXT,
                                 resource TEXT,
                                 action TEXT,
                                 duration_ms INTEGER,
                                 status INTEGER,
                                 actor TEXT, -- Serialized AdminUserDTO
                                 ip_address TEXT,
                                 user_agent TEXT,
                                 device_type TEXT,
                                 referer TEXT,
                                 geolocation TEXT, -- Serialized JSON
                                 impact TEXT, -- Serialized JSON
                                 request_details TEXT, -- Serialized JSON
                                 created_at INTEGER,
                                 payload TEXT,
                                 synced INTEGER DEFAULT 0
);

-- 4. Exceptions
CREATE TABLE kraft_exceptions (
                                  id TEXT PRIMARY KEY,
                                  trace_id TEXT NOT NULL,
                                  tenant_id TEXT,
                                  user_id TEXT,
                                  exception_class TEXT,
                                  message TEXT,
                                  stack_trace TEXT,
                                  stack_summary TEXT,
                                  path TEXT,
                                  method TEXT,
                                  status_code INTEGER,
                                  request_headers TEXT, -- Serialized JSON
                                  query_params TEXT, -- Serialized JSON
                                  host_name TEXT,
                                  environment TEXT,
                                  version TEXT,
                                  is_handled INTEGER DEFAULT 0,
                                  metadata TEXT, -- Serialized JSON
                                  created_at INTEGER,
                                  payload TEXT,
                                  synced INTEGER DEFAULT 0
);

-- 5. Query Events
CREATE TABLE kraft_query_events (
                                    id TEXT PRIMARY KEY,
                                    trace_id TEXT NOT NULL,
                                    sql TEXT,
                                    parameters TEXT, -- Serialized List
                                    query_type TEXT,
                                    entity_name TEXT,
                                    table_name TEXT,
                                    started_at INTEGER,
                                    duration_ms INTEGER,
                                    rows_affected INTEGER,
                                    rows_returned INTEGER,
                                    is_slow INTEGER DEFAULT 0,
                                    is_n_plus_one INTEGER DEFAULT 0,
                                    data_source TEXT,
                                    database_product TEXT,
                                    schema TEXT,
                                    tenant_id TEXT,
                                    thread_name TEXT,
                                    isolation_level TEXT,
                                    is_read_only INTEGER DEFAULT 0,
                                    is_batch INTEGER DEFAULT 0,
                                    batch_size INTEGER,
                                    transaction_id TEXT,
                                    execution_plan TEXT,
                                    error_details TEXT, -- Serialized QueryError
                                    created_at INTEGER,
                                    payload TEXT,
                                    synced INTEGER DEFAULT 0
);

-- Indexes for performance
CREATE INDEX idx_http_client_trace ON kraft_http_client_events(trace_id);
CREATE INDEX idx_http_client_created ON kraft_http_client_events(created_at, synced);

CREATE INDEX idx_telemetry_trace ON kraft_telemetry(trace_id);
CREATE INDEX idx_telemetry_sync ON kraft_telemetry(synced, created_at);

CREATE INDEX idx_query_trace ON kraft_query_events(trace_id);
CREATE INDEX idx_query_sync ON kraft_query_events(synced, created_at);

CREATE INDEX idx_exc_trace ON kraft_exceptions(trace_id);
CREATE INDEX idx_task_trace ON kraft_tasks(trace_id);