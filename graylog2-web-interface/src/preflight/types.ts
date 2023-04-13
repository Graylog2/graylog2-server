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
import type { CONFIGURATION_STEPS, DATA_NODES_STATUS } from 'preflight/Constants';

export type DataNode = {
  id: string,
  name: string,
  altNames: Array<string>,
  transportAddress: string,
  status: DataNodeStatus
}

export type DataNodes = Array<DataNode>;

export type DataNodesCAStatus = {
  isConfigured: boolean
}

export type DataNodeStatus = typeof DATA_NODES_STATUS[keyof typeof DATA_NODES_STATUS]['key']

export type ConfigurationStep = typeof CONFIGURATION_STEPS[keyof typeof CONFIGURATION_STEPS]['key']
