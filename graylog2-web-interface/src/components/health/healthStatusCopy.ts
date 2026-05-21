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
import type { HealthStatus } from './HealthReport.types';

export const STATUS_ORDER: HealthStatus[] = ['healthy', 'warning', 'critical', 'unknown'];

export const STATUS_LABELS: Record<HealthStatus, string> = {
  healthy: 'Healthy',
  warning: 'Warning',
  critical: 'Critical',
  unknown: 'Unknown',
};

export const STATUS_DESCRIPTION: Record<HealthStatus, string> = {
  healthy: 'Functioning properly.',
  warning: 'Experiencing a problem that needs attention.',
  critical: 'Severe issues that are negatively impacting functionality.',
  unknown: 'The state could not be evaluated.',
};

export const getStatusMeta = (status: HealthStatus) => ({
  label: STATUS_LABELS[status],
  description: STATUS_DESCRIPTION[status],
});
