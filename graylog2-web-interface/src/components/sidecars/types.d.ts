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
export type Configuration = {
  template: string;
  color: string;
  collector_id: string;
  name: string;
  id: string;
  tags: string[];
}

export type Collector = {
  service_type: string;
  node_operating_system: string;
  name: string;
  validation_parameters: string;
  executable_path: string;
  execute_parameters: string;
  default_template: string;
  id: string;
}

export type CollectorStatus = {
  verbose_message: string;
  collector_id: string;
  message: string;
  configuration_id: string;
  status: number;
}

export type ConfigurationAssignment = {
  assigned_from_tags: string[];
  collector_id: string;
  configuration_id: string;
}

export type NodeLogFile = {
  path: string;
  mod_time: string;
  size: number;
  is_dir: boolean;
}

export type SidecarSummary = {
  node_details: NodeDetails;
  assignments: ConfigurationAssignment[];
  collectors: string[];
  last_seen: string;
  sidecar_version: string;
  node_name: string;
  active: boolean;
  node_id: string;
}

export type NodeMetrics = {
  cpu_idle: number;
  disks_75: string[];
  load_1: number;
}

export type CollectorStatusList = {
  collectors: CollectorStatus[];
  message: string;
  status: number;
}

export type NodeDetails = {
  ip: string;
  collector_configuration_directory: string;
  operating_system: string;
  metrics: NodeMetrics;
  log_file_list: NodeLogFile[];
  status: CollectorStatusList;
  tags: string[];
}

export type ConfigurationSidecarsResponse = {
  sidecar_ids: string[];
  configuration_id: string;
}

export type SidecarCollectorPairType = {
  collector: Collector;
  sidecar: SidecarSummary;
}
