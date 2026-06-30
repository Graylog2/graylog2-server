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
import type { Source } from '../../types';

type NewSource = Omit<Source, 'id' | 'fleet_id'>;

const DEFAULT_SOURCES: NewSource[] = [
  {
    name: 'System Journal',
    description: 'Collects system journal logs via journald',
    enabled: true,
    type: 'journald',
    config: {
      priority: 'info',
      read_mode: 'end',
    },
  },
  {
    name: 'Syslog',
    description: 'Collects logs from /var/log/syslog',
    enabled: true,
    type: 'file',
    config: {
      paths: ['/var/log/syslog'],
      read_mode: 'end',
    },
  },
  {
    name: 'Windows Event Log',
    description: 'Collects Windows event logs from default channels',
    enabled: true,
    type: 'windows_event_log',
    config: {
      channels: [],
      include_default_channels: true,
      read_mode: 'end',
    },
  },
];

export default DEFAULT_SOURCES;
