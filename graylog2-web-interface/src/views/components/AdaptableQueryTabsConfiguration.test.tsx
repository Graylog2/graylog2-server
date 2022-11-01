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

import { render, screen } from 'wrappedTestingLibrary';
import React from 'react';
import Immutable, { OrderedSet } from 'immutable';
import userEvent from '@testing-library/user-event';

import AdaptableQueryTabsConfiguration from 'views/components/AdaptableQueryTabsConfiguration';
import mockAction from 'helpers/mocking/MockAction';
import { QueriesActions } from 'views/actions/QueriesActions';
import { ViewStatesActions } from 'views/stores/ViewStatesStore';
import ViewState from 'views/logic/views/ViewState';

jest.mock('views/stores/QueriesStore', () => ({ QueriesActions: {} }));
jest.mock('views/stores/ViewStatesStore', () => ({ ViewStatesActions: {} }));

const setShow = jest.fn();

describe('AdaptableQueryTabsConfiguration', () => {
  const renderConfiguration = () => render(
    <AdaptableQueryTabsConfiguration show
                                     setShow={setShow}
                                     activeQueryId="queryId-1"
                                     dashboardId="dashboard-id"
                                     queriesList={OrderedSet(
                                       [
                                         { id: 'queryId-1', title: 'Query Title 1' },
                                         { id: 'queryId-2', title: 'Query Title 2' },
                                       ])} />);

  beforeEach(() => {
    QueriesActions.setOrder = mockAction(jest.fn(() => Promise.resolve(Immutable.OrderedMap())));
    ViewStatesActions.patchQueriesTitle = mockAction(jest.fn(() => Promise.resolve(ViewState.create())));
  });

  it('should display modal window', async () => {
    renderConfiguration();

    await screen.findByText('Update Dashboard Pages Configuration');
  });

  it('should display list of tabs', async () => {
    renderConfiguration();

    await screen.findByText('Query Title 1');
    await screen.findByText('Query Title 2');
  });

  it('should run setOrder and patchQueriesTitle with correct tab order and titles on submit', async () => {
    renderConfiguration();
    const submitButton = await screen.findByTitle('Update configuration');
    userEvent.click(submitButton);

    await expect(QueriesActions.setOrder).toHaveBeenCalledWith(Immutable.OrderedSet([
      { id: 'queryId-1', title: 'Query Title 1' },
      { id: 'queryId-2', title: 'Query Title 2' },
    ]));

    await expect(ViewStatesActions.patchQueriesTitle).toHaveBeenCalledWith(Immutable.Set([
      { queryId: 'queryId-1', titlesMap: Immutable.Map({ tab: Immutable.Map({ title: 'Query Title 1' }) }) },
      { queryId: 'queryId-2', titlesMap: Immutable.Map({ tab: Immutable.Map({ title: 'Query Title 2' }) }) },
    ]));
  });
});
