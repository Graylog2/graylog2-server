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
import { fireEvent, render, screen } from 'wrappedTestingLibrary';

import OriginalViewHeader from 'views/components/views/ViewHeader';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import { createSearch } from 'fixtures/searches';
import View from 'views/logic/views/View';

const view = createSearch()
  .toBuilder()
  .id('viewId')
  .title('Some view')
  .type(View.Type.Dashboard)
  .build();

const ViewHeader = () => (
  <TestStoreProvider view={view}>
    <OriginalViewHeader />
  </TestStoreProvider>
);

describe('ViewHeader', () => {
  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  it('Render view type and title', async () => {
    render(<ViewHeader />);

    await screen.findByText('Dashboards', { exact: false });
    await screen.findByText('Some view');
  });

  it('Show edit modal on click', async () => {
    render(<ViewHeader />);

    const editButton = await screen.findByTitle('Edit dashboard Some view metadata');

    fireEvent.click(editButton);
    await screen.findByText('Editing saved dashboard', { exact: false });
  });
});
