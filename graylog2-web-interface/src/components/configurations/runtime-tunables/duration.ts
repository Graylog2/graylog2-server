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

import type { DurationUnit } from './types';

export const DEFAULT_UNITS: Array<DurationUnit> = ['seconds', 'minutes', 'hours', 'days'];

const UNIT_SECONDS: Record<DurationUnit, number> = { seconds: 1, minutes: 60, hours: 3600, days: 86400 };
const ISO_DURATION = /^P(?:(\d+)W)?(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?)?$/;

export const parseDuration = (iso: string): number | undefined => {
  const match = ISO_DURATION.exec(iso ?? '');
  if (!match || iso === 'P' || iso.endsWith('T')) {
    return undefined;
  }
  const [weeks, days, hours, minutes, seconds] = match.slice(1).map((part) => Number(part ?? 0));

  return weeks * 604800 + days * 86400 + hours * 3600 + minutes * 60 + seconds;
};

export const splitDuration = (iso: string, units: Array<DurationUnit>) => {
  const seconds = parseDuration(iso);
  if (seconds === undefined || units.length === 0) {
    return undefined;
  }
  const unit = [...units].reverse().find((candidate) => seconds % UNIT_SECONDS[candidate] === 0) ?? units[0];

  return { value: seconds / UNIT_SECONDS[unit], unit };
};

export const formatDuration = (value: number, unit: DurationUnit) => {
  switch (unit) {
    case 'days':
      return `P${value}D`;
    case 'hours':
      return `PT${value}H`;
    case 'minutes':
      return `PT${value}M`;
    default:
      return `PT${value}S`;
  }
};
