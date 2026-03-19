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
import buildMongodbProfilingActionView from './buildMongodbProfilingActionView';

describe('buildMongodbProfilingActionView', () => {
  it('returns loading labels when profiling status is not ready', () => {
    const view = buildMongodbProfilingActionView({
      action: null,
      state: 'unknown',
      profilingStatusByLevel: undefined,
      isStatusReady: false,
      isTogglingProfiling: false,
    });

    expect(view.actionLabel).toBe('Enable Profiling');
    expect(view.actionTitle).toBe('Loading MongoDB profiling status');
    expect(view.buttonLabel).toBe('Loading status...');
    expect(view.statusSummary).toBe('Loading current profiling status across MongoDB nodes...');
    expect(view.enablingProfiling).toBe(false);
  });

  it('returns enable action view when all nodes are off', () => {
    const view = buildMongodbProfilingActionView({
      action: 'enable',
      state: 'off',
      profilingStatusByLevel: { OFF: 3 },
      isStatusReady: true,
      isTogglingProfiling: false,
    });

    expect(view.actionLabel).toBe('Enable Profiling');
    expect(view.actionTitle).toBe('Set profiling to Slow Ops on all MongoDB nodes');
    expect(view.buttonLabel).toBe('Enable Profiling');
    expect(view.statusSummary).toMatch(/profiling is off for all mongodb nodes/i);
    expect(view.statusSummary).toMatch(/0\/3 nodes profiled/i);
    expect(view.enablingProfiling).toBe(true);
  });

  it('returns disable loading view when all nodes are enabled and mutation is running', () => {
    const view = buildMongodbProfilingActionView({
      action: 'disable',
      state: 'enabled',
      profilingStatusByLevel: { SLOW_OPS: 2, ALL: 1 },
      isStatusReady: true,
      isTogglingProfiling: true,
    });

    expect(view.actionLabel).toBe('Disable Profiling');
    expect(view.actionTitle).toBe('Disable profiling on all MongoDB nodes');
    expect(view.buttonLabel).toBe('Disabling...');
    expect(view.statusSummary).toMatch(/profiling is on for all mongodb nodes/i);
    expect(view.statusSummary).toMatch(/3\/3 nodes profiled/i);
    expect(view.enablingProfiling).toBe(false);
  });
});
