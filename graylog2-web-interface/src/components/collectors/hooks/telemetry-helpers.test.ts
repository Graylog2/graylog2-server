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
  comparePort,
  classifyInputState,
} from './telemetry-helpers';

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

describe('comparePort', () => {
  it('returns true when settings port matches input bind_address port', () => {
    const input = { attributes: { bind_address: '0.0.0.0', port: 5555 } };

    expect(comparePort(5555, input)).toBe(true);
  });

  it('returns false when settings port differs', () => {
    const input = { attributes: { bind_address: '0.0.0.0', port: 5555 } };

    expect(comparePort(443, input)).toBe(false);
  });

  it('returns null when input is undefined', () => {
    expect(comparePort(5555, undefined)).toBeNull();
  });

  it('returns null when port is missing on input', () => {
    const input = { attributes: { bind_address: '0.0.0.0' } };

    expect(comparePort(5555, input as never)).toBeNull();
  });
});

describe('classifyInputState', () => {
  it('returns not_configured when input is undefined', () => {
    expect(classifyInputState(undefined, undefined)).toBe('not_configured');
  });

  it('returns unknown when input is set but inputStates missing', () => {
    expect(classifyInputState({ id: 'i1' } as never, undefined)).toBe('unknown');
  });

  it('returns running when inputStates has a RUNNING state for the input', () => {
    const input = { id: 'i1' };
    const inputStates = [{ id: 'i1', state: 'RUNNING' }];

    expect(classifyInputState(input as never, inputStates as never)).toBe('running');
  });

  it('returns failed when inputStates has a FAILED state', () => {
    const input = { id: 'i1' };
    const inputStates = [{ id: 'i1', state: 'FAILED' }];

    expect(classifyInputState(input as never, inputStates as never)).toBe('failed');
  });

  it('returns stopped when inputStates has a STOPPED state', () => {
    const input = { id: 'i1' };
    const inputStates = [{ id: 'i1', state: 'STOPPED' }];

    expect(classifyInputState(input as never, inputStates as never)).toBe('stopped');
  });

  it('returns unknown when inputStates has no matching entry', () => {
    const input = { id: 'i1' };
    const inputStates = [{ id: 'i2', state: 'RUNNING' }];

    expect(classifyInputState(input as never, inputStates as never)).toBe('unknown');
  });
});
