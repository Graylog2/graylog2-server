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
import { SystemNotifications } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import type { SearchParams } from 'stores/PaginationTypes';

import { fetchNotifications, keyFn } from './useNotificationsList';

jest.mock('@graylog/server-api', () => ({
  SystemNotifications: { getPaginated: jest.fn() },
}));

const getPaginatedMock = asMock(SystemNotifications.getPaginated);

const baseParams: SearchParams = {
  page: 1,
  pageSize: 20,
  query: '',
  filters: undefined,
  sort: { attributeId: 'timestamp', direction: 'desc' },
};

describe('fetchNotifications', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('maps getPaginated response to list/pagination/attributes shape', async () => {
    getPaginatedMock.mockResolvedValue({
      elements: [{ id: 'n-1' }],
      pagination: { total: 1 },
      attributes: [],
    } as any);

    const result = await fetchNotifications(baseParams);

    expect(result.list).toEqual([{ id: 'n-1' }]);
    expect(result.pagination.total).toBe(1);
    expect(getPaginatedMock).toHaveBeenCalledWith(1, 20, '', undefined, 'timestamp', 'desc');
  });

  it('returns an empty page silently on 404 instead of throwing', async () => {
    getPaginatedMock.mockRejectedValue({ status: 404 });

    const result = await fetchNotifications(baseParams);

    expect(result.list).toEqual([]);
    expect(result.pagination).toEqual({ total: 0 });
  });

  it('re-throws non-404 errors', async () => {
    getPaginatedMock.mockRejectedValue({ status: 500 });

    await expect(fetchNotifications(baseParams)).rejects.toMatchObject({ status: 500 });
  });
});

describe('keyFn', () => {
  it('includes search params in the key so different params get distinct cache entries', () => {
    const key1 = keyFn(baseParams);
    const key2 = keyFn({ ...baseParams, page: 2 });

    expect(key1).not.toEqual(key2);
    expect(key1[0]).toEqual(key2[0]);
  });
});
