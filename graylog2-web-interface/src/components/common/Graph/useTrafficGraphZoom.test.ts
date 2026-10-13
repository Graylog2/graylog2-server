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
import { act, renderHook } from 'wrappedTestingLibrary/hooks';

import asMock from 'helpers/mocking/AsMock';
import AppConfig from 'util/AppConfig';

import type { Traffic } from './types';
import useTrafficGraphZoom from './useTrafficGraphZoom';

const sampleTraffic: Traffic = {
  '2026-04-07T00:00:00.000Z': 1024,
  '2026-04-08T00:00:00.000Z': 2048,
  '2026-04-09T00:00:00.000Z': 4096,
};

describe('useTrafficGraphZoom', () => {
  beforeEach(() => {
    asMock(AppConfig.isCloud).mockReturnValue(false);
  });

  it('zooms to the data on the first trigger and fully resets on the second when a limit is set', () => {
    const { result } = renderHook(() => useTrafficGraphZoom(sampleTraffic, 1024 * 1024));

    expect(result.current.zoomedToData).toBe(false);
    const initialRevision = result.current.uiRevision;

    act(() => result.current.onZoomReset());
    expect(result.current.zoomedToData).toBe(true);
    expect(result.current.uiRevision).toBe(initialRevision);

    act(() => result.current.onZoomReset());
    expect(result.current.zoomedToData).toBe(false);
    expect(result.current.uiRevision).toBe(initialRevision + 1);
  });

  it('does nothing on a pristine graph without a traffic limit', () => {
    const { result } = renderHook(() => useTrafficGraphZoom(sampleTraffic, undefined));

    const initialRevision = result.current.uiRevision;

    act(() => result.current.onZoomReset());
    expect(result.current.zoomedToData).toBe(false);
    expect(result.current.uiRevision).toBe(initialRevision);
  });

  it('resets a drag-zoomed graph with a single trigger when there is no traffic limit', () => {
    const { result } = renderHook(() => useTrafficGraphZoom(sampleTraffic, undefined));

    const initialRevision = result.current.uiRevision;

    act(() => result.current.onUserZoom());
    act(() => result.current.onZoomReset());

    expect(result.current.zoomedToData).toBe(false);
    expect(result.current.uiRevision).toBe(initialRevision + 1);
  });

  it('fully resets with a single trigger after the user has drag-zoomed', () => {
    const { result } = renderHook(() => useTrafficGraphZoom(sampleTraffic, 1024 * 1024));

    const initialRevision = result.current.uiRevision;

    act(() => result.current.onUserZoom());
    act(() => result.current.onZoomReset());

    expect(result.current.zoomedToData).toBe(false);
    expect(result.current.uiRevision).toBe(initialRevision + 1);
  });

  it('fully resets with a single trigger when drag-zoomed on top of the data zoom', () => {
    const { result } = renderHook(() => useTrafficGraphZoom(sampleTraffic, 1024 * 1024));

    const initialRevision = result.current.uiRevision;

    act(() => result.current.onZoomReset());
    expect(result.current.zoomedToData).toBe(true);

    act(() => result.current.onUserZoom());
    act(() => result.current.onZoomReset());

    expect(result.current.zoomedToData).toBe(false);
    expect(result.current.uiRevision).toBe(initialRevision + 1);
  });

  it('zooms to the data on a single trigger after plotly itself reset a drag-zoom', () => {
    const { result } = renderHook(() => useTrafficGraphZoom(sampleTraffic, 1024 * 1024));

    act(() => result.current.onUserZoom());
    act(() => result.current.onUserZoomReset());

    act(() => result.current.onZoomReset());

    expect(result.current.zoomedToData).toBe(true);
  });

  it('reports the zoom trigger as not actionable on a pristine graph without a traffic limit', () => {
    const { result } = renderHook(() => useTrafficGraphZoom(sampleTraffic, undefined));

    expect(result.current.canZoomOrReset).toBe(false);

    act(() => result.current.onUserZoom());

    expect(result.current.canZoomOrReset).toBe(true);
  });

  it('reports the zoom trigger as actionable when a limit dwarfs the data', () => {
    const { result } = renderHook(() => useTrafficGraphZoom(sampleTraffic, 1024 * 1024));

    expect(result.current.canZoomOrReset).toBe(true);
  });

  it('reports the zoom trigger as not actionable when the plotted data reaches the limit', () => {
    const { result } = renderHook(() => useTrafficGraphZoom(sampleTraffic, 2048));

    expect(result.current.canZoomOrReset).toBe(false);
  });

  it('is inert while the traffic series is still loading', () => {
    const { result } = renderHook(() => useTrafficGraphZoom(undefined, 1024 * 1024));

    expect(result.current.canZoomOrReset).toBe(false);

    act(() => result.current.onZoomReset());

    expect(result.current.zoomedToData).toBe(false);
  });

  it('never offers zoom-to-data for an empty plotted series', () => {
    const { result } = renderHook(() => useTrafficGraphZoom({}, 1024 * 1024));

    expect(result.current.canZoomOrReset).toBe(false);

    act(() => result.current.onZoomReset());

    expect(result.current.zoomedToData).toBe(false);
  });

  it('never offers zoom-to-data on cloud, even when a limit dwarfs the data', () => {
    asMock(AppConfig.isCloud).mockReturnValue(true);

    const { result } = renderHook(() => useTrafficGraphZoom(sampleTraffic, 1024 * 1024));

    expect(result.current.canZoomOrReset).toBe(false);

    act(() => result.current.onZoomReset());

    expect(result.current.zoomedToData).toBe(false);
  });

  it('resets the zoom state when the traffic data changes', () => {
    const { result, rerender } = renderHook(({ traffic }) => useTrafficGraphZoom(traffic, 1024 * 1024), {
      initialProps: { traffic: sampleTraffic },
    });

    act(() => result.current.onZoomReset());
    expect(result.current.zoomedToData).toBe(true);
    const revisionWhileZoomed = result.current.uiRevision;

    rerender({ traffic: { ...sampleTraffic, '2026-04-10T00:00:00.000Z': 8192 } });

    expect(result.current.zoomedToData).toBe(false);
    expect(result.current.uiRevision).toBe(revisionWhileZoomed + 1);
  });
});
