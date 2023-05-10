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
import * as React from 'react';
import { screen, render, fireEvent, waitFor } from 'wrappedTestingLibrary';

import SidecarFailureTrackingList from './SidecarFailureTrackingList';

import type { Collector, SidecarSummary } from '../types';

const sidecars = [{
  node_details: {
    ip: 'ip',
    collector_configuration_directory: 'collector_configuration_directory',
    operating_system: 'operating_system',
    metrics: {
      cpu_idle: 0,
      disks_75: [],
      load_1: 0,
    },
    log_file_list: [],
    status: {
      collectors: [],
      message: '',
      status: 2,
    },
    tags: [],
  },
  assignments: [],
  collectors: [],
  last_seen: 'last_seen',
  sidecar_version: 'sidecar_version',
  node_name: 'node_name',
  active: false,
  node_id: 'node_id',
}];
const collectors = [{
  service_type: '',
  node_operating_system: '',
  name: '',
  validation_parameters: '',
  executable_path: '',
  execute_parameters: '',
  default_template: '',
  id: '',
}];
const pagination = { total: 11, count: 10, page: 1, per_page: 25, query: '' };
const query = 'query';
const onlyActive = false;
const sort = { field: 'last_seen', order: 'desc' };
const handlePageChange = jest.fn();
const handleQueryChange = jest.fn();
const handleSortChange = jest.fn();
const toggleShowInactive = jest.fn();

const renderSUT = (sidecarsList: Array<SidecarSummary> = [], collectorsList: Array<Collector> = []) => render(
  <SidecarFailureTrackingList sidecars={sidecarsList}
                              collectors={collectorsList}
                              pagination={pagination}
                              query={query}
                              onlyActive={onlyActive}
                              sort={sort}
                              onPageChange={handlePageChange}
                              onQueryChange={handleQueryChange}
                              onSortChange={handleSortChange}
                              toggleShowInactive={toggleShowInactive} />,
);

describe('SidecarFailureTrackingList', () => {
  it('should render empty list', async () => {
    renderSUT();

    await screen.findByText(/There are no sidecars with failures/);
  });

  it('should render list and allow filter list', async () => {
    renderSUT(sidecars, collectors);

    await screen.findByText('Sidecar');

    const searchInput = await screen.findByPlaceholderText(/find sidecars/i);
    fireEvent.change(searchInput, { target: { value: 'demo' } });

    await waitFor(() => expect(handleQueryChange).toHaveBeenCalledWith('demo', expect.anything()));
  });

  it('should toggle inactive sidecar', async () => {
    renderSUT(sidecars, collectors);

    await screen.findByText('Sidecar');

    const hideActiveButton = screen.getByRole('button', { name: 'Hide inactive sidecars' });

    fireEvent.click(hideActiveButton);
    await waitFor(() => expect(toggleShowInactive).toHaveBeenCalled());
  });

  it('should handle sort', async () => {
    renderSUT(sidecars, collectors);

    await screen.findByText('Sidecar');

    const sortByNameButton = screen.getByTitle('node_name');
    const sortByLastSeenButton = screen.getByTitle('last_seen');

    fireEvent.click(sortByNameButton);
    await waitFor(() => expect(handleSortChange).toHaveBeenCalledWith('node_name'));

    fireEvent.click(sortByLastSeenButton);

    await waitFor(() => expect(handleSortChange).toHaveBeenCalledWith('last_seen'));
  });
});
