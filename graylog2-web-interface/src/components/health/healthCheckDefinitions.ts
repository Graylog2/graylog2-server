/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import Routes from 'routing/Routes';

const EnterpriseRoutes = {
  ARCHIVES: '/system/archives',
  DATA_LAKE: '/data-lake/setup',
  FORWARDERS: '/system/forwarders',
  REPORTS: '/reports',
} as const;

type HealthEntityListLink = {
  url: string;
  label: string;
};

type HealthCheckDefinition = {
  description?: string;
  meaning?: string;
  commonCauses?: string[];
  recommendedAction?: string;
  docsUrl?: string;
  entityList?: HealthEntityListLink;
};

const HEALTH_CHECK_DEFINITIONS: Partial<Record<string, HealthCheckDefinition>> = {
  graylog: {
    description:
      'The Graylog application itself — receives, processes, and writes log messages between sources and the search cluster.',
  },
  'graylog.server': {
    description:
      'Operational health of the running Graylog server processes — whether each node is alive, reachable, and contributing to the cluster.',
    entityList: { url: Routes.SYSTEM.CLUSTER.NODES, label: 'Graylog nodes' },
  },
  'graylog.input': {
    description: 'Health of message ingest — whether configured inputs are open and accepting data from their sources.',
    entityList: { url: Routes.SYSTEM.INPUTS, label: 'inputs' },
  },
  'graylog.processing': {
    description: 'Health of the in-flight message processing pipeline between ingest and output.',
    entityList: { url: Routes.SYSTEM.CLUSTER.NODES, label: 'Graylog nodes' },
  },
  'graylog.output': {
    description:
      'Health of message delivery and outputs — writing processed messages to storage, configured external destinations, and generated exports.',
  },
  'graylog.archiving': {
    description:
      'Health of long-term message archival — exporting indexed data to cold storage for retention or compliance.',
    entityList: { url: EnterpriseRoutes.ARCHIVES, label: 'archives' },
  },
  'graylog.data_lake': {
    description:
      'Health of Data Lake storage, preview, and retrieval for log data routed to configured Data Lake backends.',
    entityList: { url: EnterpriseRoutes.DATA_LAKE, label: 'data lake backends' },
  },
  'graylog.integrations': {
    description:
      'Health of third-party integrations Graylog relies on for authentication, notifications, and external service calls.',
  },
  search_cluster: {
    description: 'The search backend (OpenSearch or Elasticsearch) where Graylog stores and queries indexed messages.',
  },
  'search_cluster.server': {
    description: 'Operational health of each individual node in the search cluster.',
    entityList: { url: Routes.SYSTEM.CLUSTER.NODES, label: 'search nodes' },
  },
  'search_cluster.index_management': {
    description: 'Health of how Graylog manages indices across their lifecycle.',
    entityList: { url: Routes.SYSTEM.INDICES.LIST, label: 'indices' },
  },
  mongodb: {
    description: 'The configuration database that stores all Graylog settings, entities, and metadata.',
    entityList: { url: Routes.SYSTEM.CLUSTER.NODES, label: 'MongoDB nodes' },
  },
  forwarders: {
    description: 'The forwarder agents that ship messages to Graylog from remote sites or restricted networks.',
    meaning:
      'One or more forwarders are not connected, falling behind, or reporting errors. Messages from sites pointed at affected forwarders are queued locally and may be lost if their buffer fills.',
    commonCauses: [
      'Network connectivity from the forwarder to Graylog is blocked or unreliable.',
      'The forwarder process has crashed or been stopped.',
      'Authentication or TLS configuration on the forwarder is invalid.',
    ],
    recommendedAction:
      'Open the forwarders page to inspect each forwarder’s connection state, throughput, and recent errors.',
    entityList: { url: EnterpriseRoutes.FORWARDERS, label: 'forwarders' },
  },
  collectors: {
    description: 'Sidecar agents and their managed collector processes that gather and ship logs to Graylog.',
    meaning:
      'One or more Sidecars (or their managed collectors) are unreachable, out of sync with their configuration, or reporting failures. Logs from affected hosts are not arriving as expected.',
    commonCauses: [
      'The Sidecar host is offline, restarting, or disconnected from the network.',
      'The Sidecar has not picked up its latest configuration from Graylog.',
      'A managed collector process is failing (file access, parser error, shipper error).',
    ],
    recommendedAction:
      'Open the Sidecars page to inspect each Sidecar’s status, last check-in, and configuration sync.',
    entityList: { url: Routes.SYSTEM.SIDECARS.OVERVIEW, label: 'collectors' },
  },
  'graylog.server.storage': {
    description:
      'Disk usage on each Graylog server node, typically the partition holding the message journal and logs.',
    meaning:
      'Disk usage on one or more Graylog nodes is approaching or has reached its limit. If the partition fills, the journal can no longer accept new messages and ingestion stops.',
    commonCauses: [
      'Sustained backpressure causing the journal to grow.',
      'Application or system logs accumulating without rotation.',
      'A leftover snapshot, archive, or temp file consuming the partition.',
    ],
    recommendedAction: 'Open the affected node to inspect disk usage, journal size, and log retention configuration.',
  },
  'graylog.server.cpu': {
    description: 'Per-node CPU utilization across the Graylog server cluster.',
    meaning:
      'One or more Graylog nodes are running close to or above their CPU capacity, which can degrade message processing throughput and increase queue latency.',
    commonCauses: [
      'Sustained ingestion rate above the cluster’s processing capacity.',
      'An expensive pipeline rule, extractor, or stream rule consuming CPU.',
      'Other processes on the host competing for CPU.',
    ],
    recommendedAction:
      'Open the affected node to inspect its CPU profile, recent pipelines and extractors, and ingest rate.',
  },
  'graylog.server.memory': {
    description: 'Per-node JVM heap utilization across the Graylog server cluster.',
    meaning:
      'JVM heap usage on one or more Graylog nodes is at a level where garbage collection pauses or out-of-memory failures become likely.',
    commonCauses: [
      'Heap size (-Xmx) is undersized for the current ingest volume.',
      'A memory leak in a plugin or pipeline rule.',
      'Long-running searches or large dashboard widgets retaining memory.',
    ],
    recommendedAction:
      'Open the affected node to inspect heap usage, GC logs, and recent search activity. Increase JVM heap allocation if undersized.',
  },
  'graylog.server.certificates': {
    description: "Certificate validity for the Graylog server's TLS surfaces (HTTPS/API and TLS-enabled inputs).",
    meaning:
      'One or more certificates used by Graylog are expired, expiring soon, or could not be validated. Affected services may stop accepting connections or be removed from rotation.',
    commonCauses: [
      'A certificate has reached or is approaching its expiry date.',
      'A renewal job failed or has not run.',
      'The certificate inspection endpoint timed out or returned an error.',
    ],
    recommendedAction:
      'Open the affected node to verify each TLS surface (HTTPS/API and TLS-enabled inputs) and rotate certificates as needed.',
  },
  'graylog.server.processing_state': {
    description: 'Whether each Graylog node is actively processing messages or has been paused/halted.',
    meaning:
      'One or more Graylog nodes have stopped processing messages. Cluster throughput may degrade and the affected nodes’ journals will start to fill.',
    commonCauses: [
      'Processing was paused manually (System → Overview).',
      'The node crashed or its JVM is unresponsive.',
      'The node lost connectivity to MongoDB.',
    ],
    recommendedAction:
      'Open the affected node to inspect its lifecycle state, last_seen timestamp, and recent log entries.',
  },
  'graylog.server.load_balancer': {
    description:
      'Whether each Graylog node reports itself as ready to receive traffic from a load balancer (ALIVE / THROTTLED / DEAD).',
    meaning:
      'Some Graylog nodes are not healthy from the load balancer perspective and may not receive traffic as expected.',
    commonCauses: [
      'The node was drained for a graceful shutdown or maintenance.',
      'override_lb_status was set manually via the API.',
      'A lifecycle transition or journal-utilization throttling reduced the node’s LB status.',
    ],
    recommendedAction: 'Open the affected node to verify its lifecycle state and any manual lb_status override.',
  },
  'graylog.input.input_buffer': {
    description: 'The in-memory queue holding messages just received by inputs, before they enter processing.',
    meaning:
      'The input buffer is filling because messages arrive faster than they can be moved into processing. Once full, inputs stop accepting new messages; sources without retry or buffering may lose data.',
    commonCauses: [
      'Processing is paused or stalled on the node.',
      'A bottleneck downstream (processing buffer, journal, or output) is causing backpressure.',
      'A burst in ingestion rate exceeding sustained capacity.',
    ],
    recommendedAction: 'Open the affected node to check processing state and downstream buffer / journal usage.',
  },
  'graylog.input.input_failures': {
    description:
      'Configured inputs that are currently failing — not running, not accepting connections, or repeatedly crashing.',
    meaning:
      'One or more configured inputs are not running and are not accepting messages. Affected inputs may stop ingesting data; sources without retry/buffering may lose messages.',
    commonCauses: [
      'A port conflict or permission error preventing the input from binding.',
      'TLS certificate or authentication misconfiguration.',
      'An unhandled exception in the input plugin (visible in node logs).',
    ],
    recommendedAction: 'Open the inputs page to inspect the failed inputs and review their error messages.',
  },
  'graylog.processing.processing_buffer': {
    description:
      'The in-memory queue between ingest and output holding messages waiting to be processed by extractors, stream rules, and pipelines.',
    meaning:
      'Messages are accumulating in the processing buffer faster than they can be enriched and routed. Backpressure may propagate upstream to the input buffer and journal.',
    commonCauses: [
      'A slow or expensive pipeline rule, stream rule, or extractor.',
      'Output backpressure from the search cluster slowing the entire pipeline.',
      'Sustained ingest above the node’s processing capacity.',
    ],
    recommendedAction: 'Open the affected node to inspect pipeline/extractor performance and downstream output health.',
  },
  'graylog.processing.journal_size': {
    description: 'The on-disk message journal that provides a durable buffer when processing slows down or pauses.',
    meaning:
      'The journal is filling because messages arrive faster than the node can process them, or processing has been paused. If it reaches the configured max age or size, unprocessed messages can be dropped from the journal before being written.',
    commonCauses: [
      'Processing was paused on the node.',
      'A bottleneck in the search cluster is slowing index writes downstream.',
      'A pipeline rule or extractor is unusually slow under current load.',
    ],
    recommendedAction:
      'Open the affected node to check processing state, downstream indexing health, and pipeline performance.',
  },
  'graylog.output.output_buffer': {
    description:
      'The in-memory queue holding processed messages waiting to be written to storage or external destinations.',
    meaning:
      'Processed messages are queueing up faster than the configured outputs can deliver them. If the buffer fills, processing backs up and ingest may eventually stall.',
    commonCauses: [
      'The search cluster is slow to index (e.g. high CPU, full disks, shard issues).',
      'An external output (Kafka, AMQP, HTTP) is unavailable or rate-limited.',
      'Network latency between the Graylog node and its outputs.',
    ],
    recommendedAction: 'Open the affected node and inspect search cluster health and any configured external outputs.',
    entityList: { url: Routes.SYSTEM.CLUSTER.NODES, label: 'Graylog nodes' },
  },
  'graylog.output.report_generation': {
    description: 'The state of scheduled report generation jobs that produce dashboard exports and emails.',
    meaning:
      'One or more scheduled reports failed to generate or are stuck. Recipients may not receive the expected exports until the issue is resolved.',
    commonCauses: [
      'A widget or dashboard referenced by the report has been deleted or is broken.',
      'The rendering process timed out or ran out of memory.',
      'Email/SMTP delivery failed for the generated report.',
    ],
    recommendedAction: 'Open the reports page to inspect each failed report and review its job logs.',
    entityList: { url: EnterpriseRoutes.REPORTS, label: 'reports' },
  },
  'graylog.archiving.archive_failures': {
    description: 'Errors encountered while exporting indexed messages to long-term archival storage.',
    meaning:
      'One or more archive jobs failed. Messages in the affected indices have not been exported to long-term storage and may be lost when retention deletion runs.',
    commonCauses: [
      'The archive backend (filesystem path or object-storage bucket/container) is unreachable or out of space.',
      'Credentials or permissions for the archive backend are invalid.',
      'A specific index could not be read due to corruption or shard issues.',
    ],
    recommendedAction: 'Open the archives page to inspect failed archive jobs and the configured backend.',
  },
  'graylog.data_lake.connectivity': {
    description: 'The connection between Graylog and the configured Data Lake backend.',
    meaning:
      'Graylog cannot reach the configured Data Lake backend. Logs/messages destined for the Data Lake are not being delivered until connectivity is restored.',
    commonCauses: [
      'The Data Lake backend is unreachable from the Graylog host.',
      'Credentials for the Data Lake backend are invalid or expired.',
      'A network firewall or proxy is blocking the connection.',
    ],
    recommendedAction: 'Open the Data Lake page to verify the backend configuration and connectivity.',
  },
  'graylog.data_lake.message_drops': {
    description:
      'Messages lost or dropped before reaching the Data Lake, typically due to backpressure or storage errors.',
    meaning:
      'Some messages were dropped before being written to the Data Lake. Data has been lost and cannot be recovered unless the source replays it.',
    commonCauses: [
      'Backpressure from the Data Lake backend during a write spike.',
      'Transient storage errors (5xx responses) from the object store.',
      'A queue between the pipeline and the Data Lake exceeded its configured limit.',
    ],
    recommendedAction: 'Open the Data Lake page to inspect recent write errors and the configured backend.',
  },
  'graylog.integrations.idp_sync': {
    description: 'Synchronization with the configured identity provider for users, groups, and role mappings.',
    meaning:
      'Synchronization with one or more configured identity providers failed. Users, groups, or role mappings may be stale or missing until sync recovers.',
    commonCauses: [
      'The identity provider is unreachable or returning errors.',
      'Service account credentials for the IdP are invalid or expired.',
      'A schema mismatch (changed attributes, removed groups) is breaking the sync.',
    ],
    recommendedAction:
      'Open the authentication services page to inspect each backend’s last sync result and configuration.',
    entityList: { url: Routes.SYSTEM.AUTHENTICATION.BACKENDS.OVERVIEW, label: 'authentication services' },
  },
  'graylog.integrations.email_transport': {
    description: 'The configured SMTP transport used to send notifications and alerts.',
    meaning:
      'Graylog cannot deliver email through the configured SMTP transport. Notifications and report deliveries that depend on email will fail until it is restored.',
    commonCauses: [
      'The SMTP host is unreachable or refusing connections.',
      'SMTP credentials are invalid or have expired.',
      'TLS handshake with the SMTP host is failing.',
    ],
    recommendedAction: 'Open the configurations page to verify the SMTP transport settings and test connectivity.',
    entityList: { url: Routes.SYSTEM.CONFIGURATIONS, label: 'configurations' },
  },
  'search_cluster.server.storage': {
    description: 'Disk usage on each search cluster node — the partition holding indexed message data.',
    meaning:
      'One or more search cluster nodes are running out of disk space. When the disk watermark is hit, indices on that node go read-only and ingestion stops for affected shards.',
    commonCauses: [
      'Retention policy is too long for the available disk capacity.',
      'A spike in ingestion volume that hasn’t been compensated for.',
      'Old or orphaned indices were not deleted as expected.',
    ],
    recommendedAction: 'Open the search nodes page to inspect per-node disk usage and review retention configuration.',
  },
  'search_cluster.server.cpu': {
    description: 'Per-node CPU utilization across the search cluster.',
    meaning:
      'One or more search cluster nodes are under sustained CPU pressure, which slows both indexing and search queries.',
    commonCauses: [
      'Heavy or long-running search queries (large dashboards, expensive aggregations).',
      'Concurrent indexing volume above the cluster’s capacity.',
      'Background tasks (merges, snapshots, recovery) consuming CPU.',
    ],
    recommendedAction:
      'Open the search nodes page to inspect per-node load, recent slow queries, and ongoing background tasks.',
  },
  'search_cluster.server.memory': {
    description: 'Per-node JVM heap and system memory utilization across the search cluster.',
    meaning:
      'JVM heap or system memory pressure on one or more search nodes is high. Heap pressure causes GC pauses; low system memory shrinks the OS file cache and slows queries.',
    commonCauses: [
      'JVM heap is undersized for the active data and query workload.',
      'A specific aggregation, fielddata, or shard size is consuming excessive heap.',
      'System memory is undersized — leaving too little room for the OS file cache.',
    ],
    recommendedAction: 'Open the search nodes page to inspect heap usage, fielddata, and host-level memory.',
  },
  'search_cluster.server.certificates': {
    description: "Certificate validity for the search cluster's TLS surfaces (transport, REST API, mTLS to Graylog).",
    meaning:
      'One or more certificates used by the search cluster are expired, expiring soon, or could not be validated. Affected TLS surfaces may stop accepting connections.',
    commonCauses: [
      'A certificate has reached or is approaching its expiry date.',
      'A renewal job (data-node mTLS, transport CA) failed or has not run.',
      'The certificate inspection endpoint timed out or returned an error.',
    ],
    recommendedAction: 'Open the search nodes page to verify each TLS surface and rotate certificates as needed.',
  },
  'search_cluster.server.state': {
    description: 'Whether each node is up and reachable, and whether the cluster as a whole has a healthy quorum.',
    meaning:
      'One or more search cluster nodes are unreachable or the cluster is missing master-eligible quorum. Search and indexing may be partially or fully impacted until the cluster recovers.',
    commonCauses: [
      'A node has crashed or been stopped.',
      'A network partition between cluster nodes.',
      'Insufficient master-eligible nodes online to form a quorum.',
    ],
    recommendedAction: 'Open the search nodes page to inspect each node’s state and recent cluster events.',
  },
  'search_cluster.index_management.rotation': {
    description: 'Whether indices are rotating when the configured rotation threshold is met (size, time, or count).',
    meaning:
      'One or more index sets are not rotating when their configured threshold is met. Active indices may grow beyond their target size, slowing queries and risking index-level limits.',
    commonCauses: [
      'The rotation strategy threshold (size or time) is misconfigured.',
      'A previous rotation job failed and was not retried.',
      'Cluster pressure (disk, memory, shard count) is preventing new index creation.',
    ],
    recommendedAction: 'Open the indices page to inspect the affected index set and its rotation strategy.',
  },
  'search_cluster.index_management.retention_delete': {
    description: 'Whether old indices are being deleted on schedule per the retention policy.',
    meaning:
      'One or more index sets are not deleting old indices on schedule. Disk usage will continue to grow beyond the intended retention window.',
    commonCauses: [
      'The retention policy is misconfigured or paused.',
      'Old indices are locked open by a snapshot, archive, or close operation.',
      'A retention deletion job failed and has not been retried.',
    ],
    recommendedAction: 'Open the indices page to inspect the affected index set and its retention configuration.',
  },
  'search_cluster.index_management.warm_tier_move': {
    description: 'Whether indices are being demoted to warm-tier storage per the data tiering configuration.',
    meaning:
      'One or more indices that should have moved to the warm tier have not. Hot-tier disks may fill faster than expected and query performance for older data may differ from the configured tiering.',
    commonCauses: [
      'A previous tier-move job failed and was not retried.',
      'The warm storage repository is unreachable, full, or misconfigured.',
    ],
    recommendedAction: 'Open the indices page to inspect the affected index sets and the warm-tier configuration.',
  },
  'search_cluster.index_management.shard_count': {
    description:
      'The total number of shards on the cluster relative to recommended limits — too many shards degrade performance.',
    meaning:
      'The total shard count is above the recommended limit for the cluster size. Each shard consumes memory and metadata overhead; excess shards slow searches and increase recovery time.',
    commonCauses: [
      'Index sets configured with too many primary shards for the data volume.',
      'Retention is too long, accumulating many small daily indices.',
      'Shard replicas are higher than required for the deployment’s redundancy needs.',
    ],
    recommendedAction:
      'Open the indices page to review per-index-set shard configuration and consolidate where possible.',
  },
  'mongodb.connectivity': {
    description: 'Whether Graylog can reach and authenticate with the MongoDB instance or replica set.',
    meaning:
      'Graylog cannot reach or authenticate with MongoDB. Configuration reads/writes are failing, which blocks most administrative operations until connectivity is restored.',
    commonCauses: [
      'A MongoDB node has stopped or been restarted.',
      'Credentials configured in graylog.conf are invalid or have been rotated.',
      'Network connectivity between Graylog and MongoDB is blocked.',
    ],
    recommendedAction:
      'Open the cluster page to inspect MongoDB node state and verify the configured connection string.',
  },
  'mongodb.primary_state': {
    description: 'Whether the MongoDB replica set has a healthy primary node accepting writes.',
    meaning:
      'No primary is currently elected in the MongoDB replica set, so Graylog cannot persist any configuration changes (entities, users, dashboards, etc.) until a primary returns.',
    commonCauses: [
      'A network partition between MongoDB nodes is preventing election quorum.',
      'The previous primary became unreachable and a new one has not yet been elected.',
      'Insufficient voting members are online.',
    ],
    recommendedAction: 'Open the cluster page to inspect each MongoDB node’s state and replication health.',
  },
  'mongodb.slow_queries': {
    description: 'Queries against MongoDB taking longer than the configured slow-query threshold.',
    meaning:
      'Queries against MongoDB are exceeding the configured slow-query threshold. Administrative operations and UI navigation may feel sluggish.',
    commonCauses: [
      'Missing or stale indexes on collections that have grown over time.',
      'A node is under CPU, memory, or disk-IO pressure.',
      'A specific collection (e.g. system messages) has grown beyond reasonable size.',
    ],
    recommendedAction: 'Open the cluster page to inspect MongoDB node health and review slow-query logs.',
  },
  'mongodb.storage': {
    description: 'Disk usage on each MongoDB node holding the database files.',
    meaning:
      'Disk usage on one or more MongoDB nodes is approaching its limit. If the partition fills, MongoDB will refuse writes and Graylog configuration changes will fail.',
    commonCauses: [
      'Large collections (system messages, audit logs), oplog growth, indexes, or fragmentation.',
      'Other processes on the host competing for disk space.',
    ],
    recommendedAction: 'Open the cluster page to inspect MongoDB disk usage and collection sizes.',
  },
};

export const getEntityListFor = (id: string): HealthEntityListLink | undefined => {
  const own = HEALTH_CHECK_DEFINITIONS[id]?.entityList;

  if (own) return own;

  const lastDot = id.lastIndexOf('.');

  return lastDot === -1 ? undefined : getEntityListFor(id.slice(0, lastDot));
};

export default HEALTH_CHECK_DEFINITIONS;
