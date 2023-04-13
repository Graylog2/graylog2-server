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

jest.mock('preflight/hooks/useDataNodes');

const availableDataNodes = [
  {
    id: 'data-node-id-1',
    name: 'data-node-name',
    transportAddress: 'transport.address1',
    altNames: [],
    status: 'UNCONFIGURED',
  },
  {
    id: 'data-node-id-2',
    name: 'data-node-name',
    altNames: [],
    transportAddress: 'transport.address2',
    status: 'UNCONFIGURED',
  },
  {
    id: 'data-node-id-3',
    name: 'data-node-name',
    altNames: [],
    transportAddress: 'transport.address3',
    status: 'UNCONFIGURED',
  },
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
