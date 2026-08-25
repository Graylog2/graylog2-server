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
import { SystemOpensearch } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import type { SearchParams } from 'stores/PaginationTypes';

import { fetchOpensearchNodes } from './fetchClusterOpensearchNodes';

jest.mock('@graylog/server-api', () => ({
  SystemOpensearch: { listNodes: jest.fn() },
}));

const searchParams: SearchParams = {
  page: 2,
  pageSize: 20,
  query: 'name:opensearch-1',
  sort: { attributeId: 'cpu_used_percent', direction: 'desc' },
};

describe('fetchOpensearchNodes', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches nodes without extending the session and maps the response', async () => {
    const node = {
      id: 'node-id-1',
      name: 'opensearch-1',
      version: '2.19.5',
      roles: ['cluster_manager', 'data'],
      jvm_heap_max: 4_294_967_296,
      jvm_heap_used_percent: 65,
      cpu_used_percent: 25,
      disk_used_percent: 40,
      disk_used: 400_000_000,
      disk_total: 1_000_000_000,
    };
    const pagination = { page: 2, per_page: 20, total: 1, count: 1 };
    const listNodes = asMock(SystemOpensearch.listNodes).mockResolvedValue({
      total: 1,
      attributes: [],
      pagination,
      elements: [node],
      query: searchParams.query,
      defaults: { sort: { id: 'name', direction: 'ASC' } },
      sort: 'cpu_used_percent',
      order: 'desc',
    });

    const result = await fetchOpensearchNodes(searchParams);

    expect(listNodes).toHaveBeenCalledWith('cpu_used_percent', 2, 20, 'name:opensearch-1', 'desc', {
      requestShouldExtendSession: false,
    });
    expect(result).toEqual({
      attributes: [],
      list: [node],
      pagination: { ...pagination, query: 'name:opensearch-1' },
    });
  });
});
