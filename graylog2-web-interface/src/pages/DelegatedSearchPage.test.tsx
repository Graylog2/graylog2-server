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
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import DelegatedSearchPage from 'pages/DelegatedSearchPage';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { viewSliceReducer } from 'views/logic/slices/viewSlice';
import { searchExecutionSliceReducer } from 'views/logic/slices/searchExecutionSlice';

describe('DelegatedSearchPage', () => {
  it('Renders other components if registered', async () => {
    const SimpleComponent = () => <div>Hello!</div>;

    PluginStore.register(new PluginManifest({}, {
      pages: {
        search: { component: SimpleComponent },
      },
      'views.reducers': [
        { key: 'view', reducer: viewSliceReducer },
        { key: 'searchExecution', reducer: searchExecutionSliceReducer },
      ],
    }));

    render(
      <TestStoreProvider>
        <DelegatedSearchPage />
      </TestStoreProvider>,
    );

    await screen.findByText('Hello!');
  });
});
