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
import {
  classifyHostname,
  classifyInputBind,
  classifyVersion,
  instanceTelemetryProps,
  sourceTelemetryProps,
} from './telemetry-helpers';

import type { CollectorInstanceView, Source } from '../types';

describe('classifyHostname', () => {
  it.each([
    ['10.0.0.5', 'ip'],
    ['192.168.1.1', 'ip'],
    ['127.0.0.1', 'ip'],
    ['::1', 'ip'],
    ['2001:db8::1', 'ip'],
    ['graylog.example.com', 'hostname'],
    ['localhost', 'hostname'],
    ['my-server', 'hostname'],
    ['', 'hostname'],
  ])('classifies %s as %s', (input, expected) => {
    expect(classifyHostname(input)).toBe(expected);
  });
});

describe('classifyInputBind', () => {
  it.each([
    ['', 'wildcard'],
    ['0.0.0.0', 'wildcard'],
    ['::', 'wildcard'],
    ['*', 'wildcard'],
    ['10.0.0.5', 'specific'],
    ['eth0.local', 'specific'],
    [undefined, 'unknown'],
    [null, 'unknown'],
  ])('classifies %p as %s', (input, expected) => {
    expect(classifyInputBind(input as string | undefined)).toBe(expected);
  });
});

describe('classifyVersion', () => {
  it('strips build metadata', () => {
    expect(classifyVersion('0.3.1-SNAPSHOT+1caa145')).toBe('0.3.1-SNAPSHOT');
  });

  it('leaves a plain version untouched', () => {
    expect(classifyVersion('1.2.0')).toBe('1.2.0');
  });

  it('returns null for missing versions', () => {
    expect(classifyVersion(null)).toBeNull();
    expect(classifyVersion(undefined)).toBeNull();
    expect(classifyVersion('')).toBeNull();
  });
});

describe('instanceTelemetryProps', () => {
  const instance = {
    instance_uid: 'uid-1',
    fleet_id: 'fleet-1',
    status: 'offline',
    has_pending_changes: true,
    version: '0.3.1-SNAPSHOT+1caa145',
  } as CollectorInstanceView;

  it('carries status and pending changes as independent dimensions', () => {
    expect(instanceTelemetryProps(instance)).toEqual({
      instance_id: 'uid-1',
      fleet_id: 'fleet-1',
      status: 'offline',
      has_pending_changes: true,
      version: '0.3.1-SNAPSHOT',
    });
  });

  it('tolerates an instance with no reported version', () => {
    expect(instanceTelemetryProps({ ...instance, version: null }).version).toBeNull();
  });
});

describe('sourceTelemetryProps', () => {
  it('matches the trio the create/update/delete source events already send', () => {
    const source = { id: 'src-1', type: 'journald', name: 'journal' } as Source;

    expect(sourceTelemetryProps(source, 'fleet-9')).toEqual({
      fleet_id: 'fleet-9',
      source_id: 'src-1',
      source_type: 'journald',
    });
  });
});
