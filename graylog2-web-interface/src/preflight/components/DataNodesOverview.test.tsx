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

import DataNodesOverview from 'preflight/components/DataNodesOverview';
import useDataNodes from 'preflight/hooks/useDataNodes';
import { asMock } from 'helpers/mocking';

jest.mock('preflight/components/DataNodesOverview');

const availableDataNodes = [
  { id: 'data-node-id-1', transportAddress: 'transport.address1', isSecured: false },
  { id: 'data-node-id-2', transportAddress: 'transport.address2', isSecured: false },
  { id: 'data-node-id-3', transportAddress: 'transport.address3', isSecured: false },
];

describe('DataNodesOverview', () => {
  beforeEach(() => {
    asMock(useDataNodes).mockReturnValue({
      data: availableDataNodes,
      isFetching: false,
      isInitialLoading: false,
      error: undefined,
    });
  });

  it('should list available data nodes', async () => {
    render(<DataNodesOverview />);

    await screen.findByText('data-node-id-1');
    await screen.findByText('transport.address1');
  });
});
