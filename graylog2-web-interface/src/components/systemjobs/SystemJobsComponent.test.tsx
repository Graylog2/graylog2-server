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
import { render, screen } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import useSystemJobs from 'components/systemjobs/useSystemJobs';

import SystemJobsComponent from './SystemJobsComponent';

jest.mock('./useSystemJobs');

const job = {
  id: '66cf617e78917f2a1d97ee6f',
  description: 'Data Lake Optimize Job action <all> running for all streams',
  name: 'org.graylog.plugins.datalake.archive.DataLakeOptimizeJob',
  info: 'Optimized data for Data Lake archives',
  node_id: '3b37ead8-ff96-4af3-a1e1-97b07d8241c1',
  started_at: '2025-11-24T12:31:42.632Z',
  execution_duration: 'PT0.003S',
  percent_complete: 0,
  provides_progress: true,
  job_status: 'runnable',
  is_cancelable: true,
};
describe('SystemJobsComponent', () => {
  it('handles node with missing job information', async () => {
    asMock(useSystemJobs).mockReturnValue({
      node1: { jobs: [job] },
      node2: {},
    });

    render(<SystemJobsComponent />);
    await screen.findByText('Optimized data for Data Lake archives');
  });
  it('handles `useSystemJobs` returning `undefined`', async () => {
    asMock(useSystemJobs).mockReturnValue(undefined);

    render(<SystemJobsComponent />);
    await screen.findByText('Optimized data for Data Lake archives');
  });
});
