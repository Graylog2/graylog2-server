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

export type PiiRule = 'email' | 'ip-address' | 'uuid' | 'object-id' | 'hostname' | 'free-text';

const FREE_TEXT_MIN_LENGTH = 40;

// Ordered from most to least specific — the first match names the rule.
const RULES: Array<[PiiRule, RegExp]> = [
  ['email', /^[^\s@]+@[^\s@]+\.[^\s@]+$/],
  ['uuid', /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i],
  ['object-id', /^[0-9a-f]{24}$/i],
  ['ip-address', /^(\d{1,3}(\.\d{1,3}){3}|[0-9a-f:]*:[0-9a-f:]+)$/i],
  // Dot-separated labels ending in an alphabetic TLD, e.g. host.example.com.
  ['hostname', /^[a-z0-9-]+(\.[a-z0-9-]+)*\.[a-z]{2,}$/i],
];

/**
 * Heuristically classifies a telemetry payload value as potentially personally identifying.
 * Returns the name of the matched rule, or null when the value looks harmless. Deliberately
 * conservative about paths and enum-like strings: slashes and short texts never match.
 */
const detectPiiSuspect = (value: unknown): PiiRule | null => {
  if (typeof value !== 'string') return null;

  // Path-like values (app_pathname etc.) are expected and would otherwise trip the hostname rule.
  if (value.includes('/')) return null;

  const match = RULES.find(([, pattern]) => pattern.test(value));
  if (match) return match[0];

  if (value.length >= FREE_TEXT_MIN_LENGTH && value.includes(' ')) return 'free-text';

  return null;
};

export default detectPiiSuspect;
