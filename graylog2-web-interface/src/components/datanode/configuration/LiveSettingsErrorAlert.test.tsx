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

import LiveSettingsErrorAlert from './LiveSettingsErrorAlert';

const REFUSED =
  'Opensearch refused stored cluster settings: cluster.routing.allocation.disk.watermark.low = 95%: low disk watermark [95%] more than high disk watermark [90%]';

describe('LiveSettingsErrorAlert', () => {
  it('renders nothing without errors', () => {
    render(<LiveSettingsErrorAlert nodeId={null} errors={{}} />);

    expect(screen.queryByText('OpenSearch did not accept some saved settings')).not.toBeInTheDocument();
  });

  it('shows an error reported by several Data Nodes once', async () => {
    render(<LiveSettingsErrorAlert nodeId={null} errors={{ 'node-1': REFUSED, 'node-2': REFUSED }} />);

    await screen.findByText('OpenSearch did not accept some saved settings');

    expect(screen.getAllByText(new RegExp('low disk watermark \\[95%\\]'))).toHaveLength(1);
    expect(screen.getByText(/reported by 2 Data Nodes/)).toBeInTheDocument();
  });

  it('links to the shared settings on the page of a single Data Node', async () => {
    render(<LiveSettingsErrorAlert nodeId="node-1" errors={{ 'node-1': REFUSED }} />);

    expect(await screen.findByRole('link', { name: 'Data Node Configuration' })).toBeInTheDocument();
    expect(screen.queryByText(/reported by/)).not.toBeInTheDocument();
  });
});
