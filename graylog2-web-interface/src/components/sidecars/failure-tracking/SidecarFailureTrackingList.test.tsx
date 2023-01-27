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
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';

import SidecarFailureTrackingList from './SidecarFailureTrackingList';

describe('SidecarFailureTrackingList', () => {
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
  const pageSizes = [25, 50, 100];

  const handlePageChange = jest.fn();
  const handleQueryChange = jest.fn();
  const handleSortChange = jest.fn();
  const toggleShowInactive = jest.fn();

  it('should render empty list', async () => {
    const { findByText } = render(
      <SidecarFailureTrackingList sidecars={[]}
                                  collectors={[]}
                                  pagination={pagination}
                                  query={query}
                                  onlyActive={onlyActive}
                                  sort={sort}
                                  pageSizes={pageSizes}
                                  onPageChange={handlePageChange}
                                  onQueryChange={handleQueryChange}
                                  onSortChange={handleSortChange}
                                  toggleShowInactive={toggleShowInactive} />,
    );

    await findByText(/There are no sidecars with failures/);
  });

  it('should render list with all features', async () => {
    const { findByText, getByRole, getByTitle, getByLabelText } = render(
      <SidecarFailureTrackingList sidecars={sidecars}
                                  collectors={collectors}
                                  pagination={pagination}
                                  query={query}
                                  onlyActive={onlyActive}
                                  sort={sort}
                                  pageSizes={pageSizes}
                                  onPageChange={handlePageChange}
                                  onQueryChange={handleQueryChange}
                                  onSortChange={handleSortChange}
                                  toggleShowInactive={toggleShowInactive} />,
    );

    await findByText('Sidecar');
    await findByText('Collector');
    await findByText('Last Seen');
    await findByText('Status');
    await findByText('Error Message');
    await findByText('Verbose Message');

    const findButton = getByRole('button', { name: 'Find' });
    const resetButton = getByRole('button', { name: 'Reset' });
    const hideActiveButton = getByRole('button', { name: 'Hide inactive sidecars' });
    const sortByNameButton = getByTitle('node_name');
    const sortByLastSeenButton = getByTitle('last_seen');
    const pageSizeInput = getByLabelText('Show');

    fireEvent.change(pageSizeInput, { target: { value: 100 } });
    await waitFor(() => expect(handlePageChange).toHaveBeenCalledWith(1, 100));

    fireEvent.click(findButton);
    await waitFor(() => expect(handleQueryChange).toHaveBeenCalledWith(query, expect.anything()));

    fireEvent.click(resetButton);
    await waitFor(() => expect(handleQueryChange).toHaveBeenCalled());

    fireEvent.click(sortByNameButton);
    await waitFor(() => expect(handleSortChange).toHaveBeenCalledWith('node_name'));

    fireEvent.click(sortByLastSeenButton);
    await waitFor(() => expect(handleSortChange).toHaveBeenCalledWith('last_seen'));

    fireEvent.click(hideActiveButton);
    await waitFor(() => expect(toggleShowInactive).toHaveBeenCalled());
  });
});
