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

import ProfilingLevelCell from './ProfilingLevelCell';

import { MongodbProfilingLevel } from '../fetchClusterMongodbNodes';

describe('<ProfilingLevelCell />', () => {
  it('renders label for slow ops profiling level values', () => {
    render(<ProfilingLevelCell profilingLevel={MongodbProfilingLevel.SLOW_OPS} />);

    expect(screen.getByText('Slow Ops')).toBeInTheDocument();
  });

  it('renders label for off profiling level values', () => {
    render(<ProfilingLevelCell profilingLevel={MongodbProfilingLevel.OFF} />);

    expect(screen.getByText('Off')).toBeInTheDocument();
  });
});
