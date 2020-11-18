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
// @flow strict
import * as React from 'react';
import { asElement, fireEvent, render } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';

import history from 'util/History';
import Routes from 'routing/Routes';
import View, { ViewStateMap } from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import Query from 'views/logic/queries/Query';
import ViewState from 'views/logic/views/ViewState';

import BigDisplayModeConfiguration from './BigDisplayModeConfiguration';

jest.mock('routing/Routes', () => ({ pluginRoute: jest.fn() }));

const search = Search.create();
const view = View.create()
  .toBuilder()
  .id('deadbeef')
  .type(View.Type.Dashboard)
  .search(search)
  .build();

const createViewWithQueries = () => {
  const queries = [
    Query.builder().id('query-id-1').build(),
    Query.builder().id('query-id-2').build(),
    Query.builder().id('other-query-id').build(),
  ];
  const states: ViewStateMap = Immutable.Map({
    'query-id-1': ViewState.create(),
    'query-id-2': ViewState.builder().titles(Immutable.fromJS({ tab: { title: 'My awesome Query tab' } })).build(),
    'other-query-id': ViewState.create(),
  });
  const searchWithQueries = search.toBuilder()
    .queries(queries)
    .build();

  return view.toBuilder()
    .state(states)
    .search(searchWithQueries)
    .build();
};

describe('BigDisplayModeConfiguration', () => {
  it('generates markup that matches snapshot', () => {
    const { container } = render(<BigDisplayModeConfiguration view={view} />);

    expect(container).toMatchSnapshot();
  });

  it('disables menu item if `disabled` prop is `true`', () => {
    const { getByText, queryByText } = render(<BigDisplayModeConfiguration view={view} disabled />);
    const menuItem = getByText('Full Screen');

    fireEvent.submit(menuItem);

    expect(queryByText('Configuring Full Screen')).toBeNull();
  });

  it('opens modal when menu item is clicked', async () => {
    const { findByText, getByText } = render(<BigDisplayModeConfiguration view={view} />);
    const menuItem = getByText('Full Screen');

    fireEvent.click(menuItem);

    await findByText('Configuring Full Screen');
  });

  it('shows open modal per default if `open` prop is `true`', () => {
    const { getByText } = render(<BigDisplayModeConfiguration view={view} show />);

    expect(getByText('Configuring Full Screen')).not.toBeNull();
  });

  it('shows all query titles in modal', () => {
    const viewWithQueries = createViewWithQueries();
    const { getByText } = render(<BigDisplayModeConfiguration view={viewWithQueries} show />);

    expect(getByText('Page#1')).not.toBeNull();
    expect(getByText('My awesome Query tab')).not.toBeNull();
    expect(getByText('Page#3')).not.toBeNull();
  });

  it('should not allow strings for the refresh interval', () => {
    const { getByLabelText } = render(<BigDisplayModeConfiguration view={view} show />);

    const refreshInterval = asElement(getByLabelText('Refresh Interval'), HTMLInputElement);

    fireEvent.change(refreshInterval, { target: { value: 'a string' } });

    expect(refreshInterval.value).toBe('');
  });

  it('should not allow strings for the cycle interval', () => {
    const { getByLabelText } = render(<BigDisplayModeConfiguration view={view} show />);

    const cycleInterval = asElement(getByLabelText('Tab cycle interval'), HTMLInputElement);

    fireEvent.change(cycleInterval, { target: { value: 'a string' } });

    expect(cycleInterval.value).toBe('');
  });

  describe('redirects to tv mode page', () => {
    beforeEach(() => {
      history.push = jest.fn();
      Routes.pluginRoute = jest.fn(() => (viewId) => `/dashboards/tv/${viewId}`);
    });

    it('on form submit', () => {
      const { getByTestId } = render(<BigDisplayModeConfiguration view={view} show />);
      const form = getByTestId('modal-form');

      expect(form).not.toBeNull();

      fireEvent.submit(form);

      expect(Routes.pluginRoute).toHaveBeenCalledWith('DASHBOARDS_TV_VIEWID');
      expect(history.push).toHaveBeenCalledWith('/dashboards/tv/deadbeef?interval=30&refresh=10');
    });

    it('including changed refresh interval', () => {
      const { getByLabelText, getByTestId } = render(<BigDisplayModeConfiguration view={view} show />);

      const refreshInterval = getByLabelText('Refresh Interval');

      fireEvent.change(refreshInterval, { target: { value: 42 } });

      const form = getByTestId('modal-form');

      fireEvent.submit(form);

      expect(Routes.pluginRoute).toHaveBeenCalledWith('DASHBOARDS_TV_VIEWID');
      expect(history.push).toHaveBeenCalledWith('/dashboards/tv/deadbeef?interval=30&refresh=42');
    });

    it('including tab cycle interval setting', () => {
      const { getByLabelText, getByTestId } = render(<BigDisplayModeConfiguration view={view} show />);

      const cycleInterval = getByLabelText('Tab cycle interval');

      fireEvent.change(cycleInterval, { target: { value: 4242 } });

      const form = getByTestId('modal-form');

      fireEvent.submit(form);

      expect(Routes.pluginRoute).toHaveBeenCalledWith('DASHBOARDS_TV_VIEWID');
      expect(history.push).toHaveBeenCalledWith('/dashboards/tv/deadbeef?interval=4242&refresh=10');
    });

    it('including selected tabs', () => {
      const viewWithQueries = createViewWithQueries();
      const { getByLabelText, getByTestId } = render(<BigDisplayModeConfiguration view={viewWithQueries} show />);

      const query1 = getByLabelText('Page#1');

      fireEvent.click(query1);

      const form = getByTestId('modal-form');

      fireEvent.submit(form);

      expect(Routes.pluginRoute).toHaveBeenCalledWith('DASHBOARDS_TV_VIEWID');
      expect(history.push).toHaveBeenCalledWith('/dashboards/tv/deadbeef?interval=30&refresh=10&tabs=1%2C2');
    });
  });
});
