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

export const WARNING_THRESHOLD_MS = 600_000; // 10 minutes
export const DANGER_THRESHOLD_MS = 3_600_000; // 60 minutes

export type ProcessingTimeSeverity = 'normal' | 'warning' | 'danger';

export const processingTimeSeverity = (ms: number): ProcessingTimeSeverity => {
  if (ms >= DANGER_THRESHOLD_MS) {
    return 'danger';
  }

  if (ms >= WARNING_THRESHOLD_MS) {
    return 'warning';
  }

  return 'normal';
};

export const formatProcessingTime = (ms: number | undefined | null): string => {
  if (ms === undefined || ms === null || ms < 0 || Number.isNaN(ms)) {
    return '—';
  }

  if (ms < 1000) {
    return `${Math.round(ms)} ms`;
  }

  if (ms < 60_000) {
    return `${(ms / 1000).toFixed(1)} s`;
  }

  const totalSeconds = Math.round(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;

  return `${minutes} min ${seconds} s`;
};
