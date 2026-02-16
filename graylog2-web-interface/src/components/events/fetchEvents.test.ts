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
import { OrderedMap } from 'immutable';

import fetch from 'logic/rest/FetchProvider';
import asMock from 'helpers/mocking/AsMock';
import type { SearchParams } from 'stores/PaginationTypes';
import type { UrlQueryFilters } from 'components/common/EntityFilters/types';

import fetchEvents, { defaultTimeRange, parseFilters, parseTypeFilter } from './fetchEvents';

jest.mock('logic/rest/FetchProvider', () => jest.fn());
jest.mock('util/URLUtils', () => ({
  qualifyUrl: jest.fn((path: string) => `http://localhost${path}`),
}));

describe('fetchEvents', () => {
  const THIRTY_DAYS_IN_SECONDS = 30 * 86400;

  describe('defaultTimeRange', () => {
    it('should be 30 days', () => {
      expect(defaultTimeRange).toEqual({ type: 'relative', range: THIRTY_DAYS_IN_SECONDS });
    });
  });

  describe('parseTypeFilter', () => {
    it('returns "only" for "true"', () => {
      expect(parseTypeFilter('true')).toBe('only');
    });

    it('returns "exclude" for "false"', () => {
      expect(parseTypeFilter('false')).toBe('exclude');
    });

    it('returns "include" for undefined', () => {
      expect(parseTypeFilter(undefined)).toBe('include');
    });

    it('returns "include" for unrecognized values', () => {
      expect(parseTypeFilter('something')).toBe('include');
    });
  });

  describe('parseFilters', () => {
    it('uses provided defaultTimerange when no timestamp filter exists', () => {
      const filters: UrlQueryFilters = OrderedMap();
      const result = parseFilters(filters, defaultTimeRange);

      expect(result.timerange).toEqual({ type: 'relative', range: THIRTY_DAYS_IN_SECONDS });
    });

    it('falls back to allTime when no defaultTimerange is provided', () => {
      const filters: UrlQueryFilters = OrderedMap();
      const result = parseFilters(filters);

      expect(result.timerange).toEqual({ type: 'relative', range: 0 });
    });

    it('parses explicit timestamp filter over default', () => {
      const filters: UrlQueryFilters = OrderedMap({
        timestamp: ['2024-01-01 00:00:00.000><2024-01-31 23:59:59.000'],
      });
      const result = parseFilters(filters, defaultTimeRange);

      expect(result.timerange).toEqual({
        type: 'absolute',
        from: '2024-01-01 00:00:00.000',
        to: '2024-01-31 23:59:59.000',
      });
    });

    it('parses timerange_start into filter.aggregation_timerange', () => {
      const filters: UrlQueryFilters = OrderedMap({
        timerange_start: ['2024-01-01 00:00:00.000><2024-01-31 23:59:59.000'],
      });
      const result = parseFilters(filters, defaultTimeRange);

      expect(result.filter.aggregation_timerange).toEqual({
        type: 'absolute',
        from: '2024-01-01 00:00:00.000',
        to: '2024-01-31 23:59:59.000',
      });
    });

    it('does not include aggregation_timerange when timerange_start filter is absent', () => {
      const filters: UrlQueryFilters = OrderedMap();
      const result = parseFilters(filters, defaultTimeRange);

      expect(result.filter.aggregation_timerange).toBeUndefined();
    });

    it('parses alert filter', () => {
      const filters: UrlQueryFilters = OrderedMap({ alert: ['true'] });
      const result = parseFilters(filters);

      expect(result.filter.alerts).toBe('only');
    });

    it('includes key filter when present', () => {
      const filters: UrlQueryFilters = OrderedMap({ key: ['key1', 'key2'] });
      const result = parseFilters(filters);

      expect(result.filter.key).toEqual(['key1', 'key2']);
    });

    it('includes event_definition_id filter when present', () => {
      const filters: UrlQueryFilters = OrderedMap({ event_definition_id: ['def-1'] });
      const result = parseFilters(filters);

      expect(result.filter.event_definitions).toEqual(['def-1']);
    });

    it('includes priority filter when present', () => {
      const filters: UrlQueryFilters = OrderedMap({ priority: ['1', '2'] });
      const result = parseFilters(filters);

      expect(result.filter.priority).toEqual(['1', '2']);
    });

    it('includes id filter when present', () => {
      const filters: UrlQueryFilters = OrderedMap({ id: ['evt-1'] });
      const result = parseFilters(filters);

      expect(result.filter.id).toEqual(['evt-1']);
    });

    it('includes part_of_detection_chain filter when present', () => {
      const filters: UrlQueryFilters = OrderedMap({ part_of_detection_chain: ['true'] });
      const result = parseFilters(filters);

      expect(result.filter.part_of_detection_chain).toBe('true');
    });
  });

  describe('fetchEvents', () => {
    const baseSearchParams: SearchParams = {
      page: 1,
      pageSize: 10,
      query: '',
      sort: { attributeId: 'timestamp', direction: 'desc' },
      filters: OrderedMap(),
    };

    beforeEach(() => {
      jest.clearAllMocks();

      asMock(fetch).mockResolvedValue({
        events: [],
        total_events: 0,
        parameters: { page: 1, per_page: 10 },
        context: {},
      });
    });

    it('sends defaultTimeRange when no timestamp filter is provided', async () => {
      await fetchEvents(baseSearchParams, undefined);

      expect(fetch).toHaveBeenCalledWith(
        'POST',
        expect.any(String),
        expect.objectContaining({
          timerange: { type: 'relative', range: THIRTY_DAYS_IN_SECONDS },
        }),
      );
    });

    it('sends explicit timerange when timestamp filter is provided', async () => {
      const searchParams: SearchParams = {
        ...baseSearchParams,
        filters: OrderedMap({ timestamp: ['2024-01-01 00:00:00.000><2024-01-31 23:59:59.000'] }),
      };

      await fetchEvents(searchParams, undefined);

      expect(fetch).toHaveBeenCalledWith(
        'POST',
        expect.any(String),
        expect.objectContaining({
          timerange: {
            type: 'absolute',
            from: '2024-01-01 00:00:00.000',
            to: '2024-01-31 23:59:59.000',
          },
        }),
      );
    });

    it('concatenates streamId with query', async () => {
      await fetchEvents({ ...baseSearchParams, query: 'test' }, 'stream-1');

      expect(fetch).toHaveBeenCalledWith(
        'POST',
        expect.any(String),
        expect.objectContaining({
          query: '(test) AND source_streams:stream-1',
        }),
      );
    });

    it('uses only streamId as query when query is empty', async () => {
      await fetchEvents(baseSearchParams, 'stream-1');

      expect(fetch).toHaveBeenCalledWith(
        'POST',
        expect.any(String),
        expect.objectContaining({
          query: 'source_streams:stream-1',
        }),
      );
    });
  });
});
