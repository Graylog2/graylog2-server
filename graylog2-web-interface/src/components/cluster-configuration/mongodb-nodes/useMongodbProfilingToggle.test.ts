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
import { getClusterProfilingState, getProfilingActionForState } from './useMongodbProfilingToggle';

describe('useMongodbProfilingToggle helpers', () => {
  it('returns unknown for missing or empty status', () => {
    expect(getClusterProfilingState(undefined)).toBe('unknown');
    expect(getClusterProfilingState({})).toBe('unknown');
  });

  it('returns off when all nodes have profiling disabled', () => {
    expect(
      getClusterProfilingState({
        OFF: 3,
      }),
    ).toBe('off');
  });

  it('returns enabled when all nodes have profiling enabled', () => {
    expect(
      getClusterProfilingState({
        SLOW_OPS: 2,
        ALL: 1,
      }),
    ).toBe('enabled');
  });

  it('returns mixed when nodes have different profiling states', () => {
    expect(
      getClusterProfilingState({
        OFF: 1,
        SLOW_OPS: 1,
      }),
    ).toBe('mixed');
  });

  it('maps cluster state to expected toggle action', () => {
    expect(getProfilingActionForState('off')).toBe('enable');
    expect(getProfilingActionForState('unknown')).toBe('enable');
    expect(getProfilingActionForState('mixed')).toBe('enable');
    expect(getProfilingActionForState('enabled')).toBe('disable');
  });
});
