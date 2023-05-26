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
import { renderPreflight, screen } from 'wrappedTestingLibrary';

import DataNodesOverview from 'preflight/components/Setup/DataNodesOverview';
import useDataNodes from 'preflight/hooks/useDataNodes';
import { asMock } from 'helpers/mocking';
import FetchError from 'logic/errors/FetchError';
import { dataNodes } from 'fixtures/dataNodes';

jest.mock('preflight/hooks/useDataNodes');
jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

describe('DataNodesOverview', () => {
  let oldConfirm;

  beforeEach(() => {
    asMock(useDataNodes).mockReturnValue({
      data: dataNodes,
      isFetching: false,
      isInitialLoading: false,
      error: undefined,
    });

    oldConfirm = window.confirm;
    window.confirm = jest.fn(() => true);
  });

  afterEach(() => {
    window.confirm = oldConfirm;
  });

  it('should list available data nodes', async () => {
    renderPreflight(<DataNodesOverview />);

    await screen.findByText('node-id-3');
    await screen.findByText('http://localhost:9200');
  });

  it('should display error message when there was an error fetching data nodes', async () => {
    asMock(useDataNodes).mockReturnValue({
      data: [],
      isFetching: false,
      isInitialLoading: false,
      error: new FetchError('The request error message', 500, { status: 500, body: { message: 'The request error message' } }),
    });

    renderPreflight(<DataNodesOverview />);
    await screen.findByText(/There was an error fetching the data nodes:/);
    await screen.findByText(/There was an error fetching a resource: The request error message. Additional information: Not available/);
  });
});
