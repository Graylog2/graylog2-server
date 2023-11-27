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
import { render } from 'wrappedTestingLibrary';

import useLocation from 'routing/useLocation';
import { asMock } from 'helpers/mocking';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import TestStoreProvider from 'views/test/TestStoreProvider';

import type { DashboardPageContextType } from './DashboardPageContext';
import DashboardPageContext from './DashboardPageContext';
import DashboardPageContextProvider from './DashboardPageContextProvider';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

jest.mock('routing/useLocation', () => jest.fn(() => ({ search: '', pathname: '' })));

const emptyLocation = {
  pathname: '',
  search: '',
  hash: '',
  state: undefined,
  key: '',
};

describe('DashboardPageContextProvider', () => {
  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    asMock(useLocation).mockReturnValue(emptyLocation);
  });

  const renderSUT = (consume: (value: DashboardPageContextType) => React.ReactNode) => render((
    <TestStoreProvider>
      <DashboardPageContextProvider>
        <DashboardPageContext.Consumer>
          {consume}
        </DashboardPageContext.Consumer>
      </DashboardPageContextProvider>
    </TestStoreProvider>
  ));

  it('should update url on page set', () => {
    let contextValue;

    const consume = (value: DashboardPageContextType) => {
      contextValue = value;

      return null;
    };

    renderSUT(consume);

    contextValue.setDashboardPage('page-id');

    expect(mockNavigate).toHaveBeenCalledWith('?page=page-id', { replace: true });
  });

  it('should update url on page change', () => {
    asMock(useLocation).mockReturnValueOnce({
      ...emptyLocation,
      search: '?page=page2-id',
    });

    let contextValue;

    const consume = (value: DashboardPageContextType) => {
      contextValue = value;

      return null;
    };

    renderSUT(consume);

    contextValue.setDashboardPage('page-id');

    expect(mockNavigate).toHaveBeenCalledWith('?page=page-id', { replace: true });
  });

  it('should unset a page from url', () => {
    asMock(useLocation).mockReturnValueOnce({
      ...emptyLocation,
      search: '?page=page-id',
    });

    let contextValue;

    const consume = (value: DashboardPageContextType) => {
      contextValue = value;

      return null;
    };

    renderSUT(consume);

    contextValue.unsetDashboardPage();

    expect(mockNavigate).toHaveBeenCalledWith('', { replace: true });
  });

  it('should not set to an unknown page', () => {
    asMock(useLocation).mockReturnValueOnce({
      ...emptyLocation,
      search: '?page=page-id',
    });

    let contextValue;

    const consume = (value: DashboardPageContextType) => {
      contextValue = value;

      return null;
    };

    renderSUT(consume);

    contextValue.setDashboardPage('new');

    expect(mockNavigate).toHaveBeenCalledWith('', { replace: true });
  });
});
