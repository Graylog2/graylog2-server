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
import userEvent from '@testing-library/user-event';
import { render, screen } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';

import MongodbProfilingAction from './MongodbProfilingAction';
import useMongodbProfilingToggle from './useMongodbProfilingToggle';

jest.mock('./useMongodbProfilingToggle', () => ({
  __esModule: true,
  default: jest.fn(() => ({
    action: 'enable',
    state: 'off',
    profilingStatusByLevel: { OFF: 3 },
    isLoadingStatus: false,
    isTogglingProfiling: false,
    runToggleAction: jest.fn(),
  })),
}));

describe('<MongodbProfilingAction />', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders enable action and initializes profiling hook', () => {
    const mockUseMongodbProfilingToggle = asMock(useMongodbProfilingToggle);

    render(<MongodbProfilingAction />);

    expect(screen.getByText(/profiling helps identify slow queries/i)).toBeInTheDocument();
    expect(screen.getByText(/profiling is off for all mongodb nodes/i)).toBeInTheDocument();
    expect(screen.getByText(/0\/3 nodes profiled/i)).toBeInTheDocument();
    expect(screen.getByText('Enable Profiling')).toBeInTheDocument();
    expect(mockUseMongodbProfilingToggle).toHaveBeenCalledWith();
  });

  it('asks for confirmation when profiling is in enable mode', async () => {
    const runToggleAction = jest.fn().mockResolvedValue(undefined);
    const mockUseMongodbProfilingToggle = asMock(useMongodbProfilingToggle);
    mockUseMongodbProfilingToggle.mockReturnValue({
      action: 'enable',
      state: 'off',
      profilingStatusByLevel: { OFF: 3 },
      isLoadingStatus: false,
      isTogglingProfiling: false,
      runToggleAction,
    });

    render(<MongodbProfilingAction />);

    await userEvent.click(screen.getByText('Enable Profiling'));

    expect(runToggleAction).not.toHaveBeenCalled();
    expect(screen.getByText(/level 1, 100ms threshold/i)).toBeInTheDocument();
  });

  it('runs toggle action directly when profiling is in disable mode', async () => {
    const runToggleAction = jest.fn().mockResolvedValue(undefined);
    const mockUseMongodbProfilingToggle = asMock(useMongodbProfilingToggle);
    mockUseMongodbProfilingToggle.mockReturnValue({
      action: 'disable',
      state: 'enabled',
      profilingStatusByLevel: { SLOW_OPS: 2, ALL: 1 },
      isLoadingStatus: false,
      isTogglingProfiling: false,
      runToggleAction,
    });

    render(<MongodbProfilingAction />);

    expect(screen.getByText(/profiling is on for all mongodb nodes/i)).toBeInTheDocument();
    expect(screen.getByText(/3\/3 nodes profiled/i)).toBeInTheDocument();
    await userEvent.click(screen.getByText('Disable Profiling'));

    expect(runToggleAction).toHaveBeenCalledTimes(1);
    expect(screen.queryByText(/level 1, 100ms threshold/i)).not.toBeInTheDocument();
  });

  it('shows full distribution in mixed state', () => {
    const mockUseMongodbProfilingToggle = asMock(useMongodbProfilingToggle);
    mockUseMongodbProfilingToggle.mockReturnValue({
      action: 'enable',
      state: 'mixed',
      profilingStatusByLevel: { OFF: 2, SLOW_OPS: 1, ALL: 0 },
      isLoadingStatus: false,
      isTogglingProfiling: false,
      runToggleAction: jest.fn(),
    });

    render(<MongodbProfilingAction />);

    expect(screen.getByText(/profiling differs across mongodb nodes/i)).toBeInTheDocument();
    expect(screen.getByText(/off 2, slow_ops 1, all 0/i)).toBeInTheDocument();
  });
});
