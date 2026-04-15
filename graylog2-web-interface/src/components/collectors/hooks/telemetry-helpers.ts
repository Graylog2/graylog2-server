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
export type HostnameKind = 'ip' | 'hostname';
export type InputBindType = 'wildcard' | 'specific' | 'unknown';

const IPV4_PATTERN = /^(\d{1,3}\.){3}\d{1,3}$/;
const IPV6_PATTERN = /^[0-9a-fA-F:]+$/;

// Classifies a hostname string as an IP address or a DNS-style name.
// Empty string treated as 'hostname' (PostHog breakdown defaults are better than omitting).
export const classifyHostname = (value: string): HostnameKind => {
  if (!value) return 'hostname';
  if (IPV4_PATTERN.test(value)) return 'ip';
  if (value.includes(':') && IPV6_PATTERN.test(value)) return 'ip';

  return 'hostname';
};

// Classifies an Input's bind_address as wildcard (listens on any interface) or specific.
export const classifyInputBind = (bindAddress: string | undefined | null): InputBindType => {
  if (bindAddress === undefined || bindAddress === null) return 'unknown';
  if (bindAddress === '' || bindAddress === '0.0.0.0' || bindAddress === '::' || bindAddress === '*') {
    return 'wildcard';
  }

  return 'specific';
};
