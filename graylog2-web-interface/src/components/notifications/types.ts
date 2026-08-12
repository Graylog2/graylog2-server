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
export type NotificationKind =
  | 'data_node_needs_provisioning'
  | 'deflector_exists_as_index'
  | 'multi_master'
  | 'no_master'
  | 'es_open_files'
  | 'es_cluster_red'
  | 'es_unavailable'
  | 'no_input_running'
  | 'input_failed_to_start'
  | 'input_failing'
  | 'input_failure_shutdown'
  | 'check_server_clocks'
  | 'outdated_version'
  | 'email_transport_configuration_invalid'
  | 'email_transport_failed'
  | 'stream_processing_disabled'
  | 'gc_too_long'
  | 'journal_utilization_too_high'
  | 'journal_uncommitted_messages_deleted'
  | 'output_disabled'
  | 'output_failing'
  | 'index_ranges_recalculation'
  | 'generic'
  | 'generic_with_link'
  | 'es_index_blocked'
  | 'es_node_disk_watermark_low'
  | 'es_node_disk_watermark_high'
  | 'es_node_disk_watermark_flood_stage'
  | 'es_shard_allocation_maximum'
  | 'es_version_mismatch'
  | 'legacy_ldap_config_migration'
  | 'multi_leader'
  | 'no_leader'
  | 'archiving_summary'
  | 'search_error'
  | 'sidecar_status_unknown'
  | 'certificate_needs_renewal'
  | 'drawdown_license_error'
  | 'remote_reindex_running'
  | 'remote_reindex_finished'
  | 'data_node_version_mismatch'
  | 'data_tiering_rollover_error'
  | 'data_node_heap_warning';

export type NotificationType = {
  id: string;
  type: NotificationKind;
  key: string;
  severity: string;
  node_id: string;
  title: string;
  description: string;
  details: Record<string, unknown>;
  timestamp: string;
};
