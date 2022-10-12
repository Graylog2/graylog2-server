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
  readonly template: string;
  readonly color: string;
  readonly collector_id: string;
  readonly name: string;
  readonly id: string;
  readonly tags: string[];
}

export type Collector = {
  readonly service_type: string;
  readonly node_operating_system: string;
  readonly name: string;
  readonly validation_parameters: string;
  readonly executable_path: string;
  readonly execute_parameters: string;
  readonly default_template: string;
  readonly id: string;
}

export type CollectorStatus = {
  readonly verbose_message: string;
  readonly collector_id: string;
  readonly message: string;
  readonly configuration_id: string;
  readonly status: number;
}

export type ConfigurationAssignment = {
  readonly assigned_from_tags: string[];
  readonly collector_id: string;
  readonly configuration_id: string;
}

export type NodeLogFile = {
  readonly path: string;
  readonly mod_time: string;
  readonly size: number;
  readonly is_dir: boolean;
}

export type SidecarSummary = {
  readonly node_details: NodeDetails;
  readonly assignments: ConfigurationAssignment[];
  readonly collectors: string[];
  readonly last_seen: string;
  readonly sidecar_version: string;
  readonly node_name: string;
  readonly active: boolean;
  readonly node_id: string;
}

export type NodeMetrics = {
  readonly cpu_idle: number;
  readonly disks_75: string[];
  readonly load_1: number;
}

export type CollectorStatusList = {
  readonly collectors: CollectorStatus[];
  readonly message: string;
  readonly status: number;
}

export type NodeDetails = {
  readonly ip: string;
  readonly collector_configuration_directory: string;
  readonly operating_system: string;
  readonly metrics: NodeMetrics;
  readonly log_file_list: NodeLogFile[];
  readonly status: CollectorStatusList;
  readonly tags: string[];
}
