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
import type { ColorVariant } from 'theme/sawmill';

import type { CollectorInstanceView } from '../types';

type InstanceStatus = CollectorInstanceView['status'];

/**
 * How a collector's online/offline state is worded (translation key in the `collectors` namespace) and coloured.
 * Render via `InstanceStatusLabel` rather than reading this directly.
 */
export const INSTANCE_STATUS_LABELS: Record<InstanceStatus, { labelKey: string; style: ColorVariant }> = {
  online: { labelKey: 'instance_status.online', style: 'success' },
  offline: { labelKey: 'instance_status.offline', style: 'default' },
};

/**
 * Human names for the operating systems a collector reports. These are the agent's own values, so
 * `darwin` rather than the `macos` id the install-platform list uses. Unknown values should fall
 * back to the raw string rather than being hidden.
 */
export const OS_LABELS: Record<string, string> = {
  linux: 'Linux',
  windows: 'Windows',
  darwin: 'macOS',
};
