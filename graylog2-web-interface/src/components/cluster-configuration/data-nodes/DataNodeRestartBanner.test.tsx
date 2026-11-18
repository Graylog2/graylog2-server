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
import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import useDataNodeRestartProgress from 'components/datanode/configuration/useDataNodeRestartProgress';

import DataNodeRestartBanner from './DataNodeRestartBanner';

jest.mock('components/datanode/configuration/useDataNodeRestartProgress');

describe('<DataNodeRestartBanner />', () => {
  it('shows the progress while Data Nodes restart', () => {
    asMock(useDataNodeRestartProgress).mockReturnValue({
      isRestarting: true,
      progress: 'Restarting the Data Nodes one after another: 1 of 3 done, now restarting datanode-2.',
      markRestartRequested: jest.fn(),
    });

    render(<DataNodeRestartBanner />);

    expect(screen.getByText('Data Node restart in progress')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('1 of 3 done, now restarting datanode-2.');
    expect(useDataNodeRestartProgress).toHaveBeenCalledWith(null);
  });

  it('shows nothing while no Data Node restarts', () => {
    asMock(useDataNodeRestartProgress).mockReturnValue({
      isRestarting: false,
      progress: undefined,
      markRestartRequested: jest.fn(),
    });

    render(<DataNodeRestartBanner />);

    expect(screen.queryByText('Data Node restart in progress')).not.toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });
});
