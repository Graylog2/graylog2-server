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

import type { IndexSummary } from 'hooks/useIndexerOverview';

import IndexSection from './IndexSection';

const indexSummary = (indexName: string, bytes: number, totalBytes: number): IndexSummary => ({
  index_name: indexName,
  size: { events: 1000, deleted: 0, bytes, total_bytes: totalBytes },
  is_deflector: false,
  is_closed: false,
  is_reopened: false,
  shard_count: 1,
  tier: 'HOT',
});

const renderSection = (indices: Array<IndexSummary>) =>
  render(<IndexSection headline="Hot Indices" subheading="Indices on hot tier" indices={indices} indexDetails={[]} />);

describe('IndexSection', () => {
  it('shows size of primaries and size including replicas for the section', async () => {
    renderSection([indexSummary('graylog_0', 1024, 2048), indexSummary('graylog_1', 1024, 2048)]);

    expect(await screen.findByText('Size (primaries):')).toBeInTheDocument();
    expect(screen.getByText('2.0KiB')).toBeInTheDocument();
    expect(screen.getByText('Size (incl. replicas):')).toBeInTheDocument();
    expect(screen.getByText('4.0KiB')).toBeInTheDocument();
  });

  it('shows size including replicas of an index when it has replicas', async () => {
    renderSection([indexSummary('graylog_0', 1024, 3072)]);

    await screen.findByText(/1\.0KiB primaries, 3\.0KiB incl\. replicas \/ 1,000 messages/);
  });

  it('shows only one size of an index without replicas', async () => {
    renderSection([indexSummary('graylog_0', 1024, 1024)]);

    await screen.findByText(/\(1\.0KiB \/ 1,000 messages\)/);

    expect(screen.getByRole('heading', { name: /graylog_0/ })).not.toHaveTextContent(/incl\. replicas/);
  });
});
