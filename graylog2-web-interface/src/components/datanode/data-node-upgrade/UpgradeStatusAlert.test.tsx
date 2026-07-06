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

import UpgradeStatusAlert from './UpgradeStatusAlert';

const defaultProps = {
  currentOpenSearchVersion: '3.5.0',
  isOpenSearchVersionError: false,
  isOpenSearchUpToDate: true,
  isLoadingOpenSearchVersion: false,
  unavailableDataNodeCount: 0,
};

describe('UpgradeStatusAlert', () => {
  it('reports the embedded OpenSearch as up to date when all Data Nodes are available', () => {
    render(<UpgradeStatusAlert {...defaultProps} />);

    expect(screen.getByText(/embedded opensearch is up to date/i)).toBeInTheDocument();
    expect(screen.getByText(/\(3\.5\.0\)\./)).toBeInTheDocument();
  });

  it('does not claim up to date while Data Nodes are unavailable', () => {
    // An unavailable node may come back with a different OpenSearch version than its metadata claims, so
    // neither "up to date" nor "not up to date" would be honest.
    render(<UpgradeStatusAlert {...defaultProps} isOpenSearchUpToDate={false} unavailableDataNodeCount={2} />);

    expect(screen.getByText(/cannot be confirmed while 2 data nodes are unavailable/i)).toBeInTheDocument();
    expect(screen.queryByText(/is up to date/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/is not up to date/i)).not.toBeInTheDocument();
  });

  it('reports outdated embedded OpenSearch when all Data Nodes are available', () => {
    render(<UpgradeStatusAlert {...defaultProps} isOpenSearchUpToDate={false} />);

    expect(screen.getByText(/embedded opensearch is not up to date/i)).toBeInTheDocument();
  });
});
