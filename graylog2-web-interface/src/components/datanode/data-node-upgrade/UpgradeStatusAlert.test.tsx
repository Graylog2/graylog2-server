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
  unavailableDataNodeCount: 0,
} as const;

describe('UpgradeStatusAlert', () => {
  it('reports the embedded OpenSearch as up to date', () => {
    render(<UpgradeStatusAlert {...defaultProps} status="up-to-date" />);

    expect(screen.getByText(/embedded opensearch is up to date/i)).toBeInTheDocument();
    expect(screen.getByText(/\(3\.5\.0\)\./)).toBeInTheDocument();
  });

  it('reports outdated embedded OpenSearch', () => {
    render(<UpgradeStatusAlert {...defaultProps} status="outdated" />);

    expect(screen.getByText(/embedded opensearch is not up to date/i)).toBeInTheDocument();
  });

  it('states neither verdict while Data Nodes are unavailable', () => {
    render(<UpgradeStatusAlert {...defaultProps} status="unconfirmed" unavailableDataNodeCount={2} />);

    expect(screen.getByText(/cannot be confirmed while 2 data nodes are unavailable/i)).toBeInTheDocument();
    expect(screen.queryByText(/is up to date/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/is not up to date/i)).not.toBeInTheDocument();
  });

  it('reports a rolling upgrade in progress instead of a verdict while the job is active', () => {
    render(<UpgradeStatusAlert {...defaultProps} status="upgrading" />);

    expect(screen.getByText(/rolling upgrade is in progress/i)).toBeInTheDocument();
    expect(screen.queryByText(/is up to date/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/is not up to date/i)).not.toBeInTheDocument();
  });

  it('states neither verdict while the OpenSearch state is still loading', async () => {
    render(<UpgradeStatusAlert {...defaultProps} status="checking" />);

    expect(await screen.findByText(/checking opensearch status/i)).toBeInTheDocument();
    expect(screen.queryByText(/is up to date/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/is not up to date/i)).not.toBeInTheDocument();
  });

  it('reports when the OpenSearch version could not be checked', () => {
    render(<UpgradeStatusAlert {...defaultProps} status="error" />);

    expect(screen.getByText(/could not check data nodes/i)).toBeInTheDocument();
  });
});
