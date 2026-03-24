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

import PercentRatioCell from './PercentRatioCell';

describe('PercentRatioCell', () => {
  it('renders N/A when percent is missing', () => {
    render(<PercentRatioCell percent={null} warningThreshold={0.7} dangerThreshold={0.9} />);

    expect(screen.getByText('N/A')).toBeInTheDocument();
  });

  it('renders formatted percentage when percent is provided', () => {
    render(<PercentRatioCell percent={75} warningThreshold={0.7} dangerThreshold={0.9} />);

    expect(screen.getByText('75.00%')).toBeInTheDocument();
  });
});
