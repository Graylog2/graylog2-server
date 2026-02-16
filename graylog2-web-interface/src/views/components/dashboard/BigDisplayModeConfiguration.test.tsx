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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { asElement, render, screen } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';
import type { Optional } from 'utility-types';

import type { ViewStateMap } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import Query from 'views/logic/queries/Query';
import ViewState from 'views/logic/views/ViewState';
import mockHistory from 'helpers/mocking/mockHistory';
import { asMock } from 'helpers/mocking';
import useHistory from 'routing/useHistory';
import wrapWithMenu from 'helpers/components/wrapWithMenu';

import BigDisplayModeConfiguration from './BigDisplayModeConfiguration';

jest.mock('routing/useHistory');

const search = Search.create();
const view = View.create().toBuilder().id('deadbeef').type(View.Type.Dashboard).search(search).build();

const createViewWithQueries = () => {
  const queries = [
    Query.builder().id('query-id-1').build(),
    Query.builder().id('query-id-2').build(),
    Query.builder().id('other-query-id').build(),
  ];
  const states: ViewStateMap = Immutable.Map({
    'query-id-1': ViewState.create(),
    'query-id-2': ViewState.builder()
      .titles(Immutable.fromJS({ tab: { title: 'My awesome Query tab' } }))
      .build(),
    'other-query-id': ViewState.create(),
  });
  const searchWithQueries = search.toBuilder().queries(queries).build();

  return view.toBuilder().state(states).search(searchWithQueries).build();
};

describe('BigDisplayModeConfiguration', () => {
  const SUT = wrapWithMenu((props: Optional<React.ComponentProps<typeof BigDisplayModeConfiguration>, 'view'>) => (
    <BigDisplayModeConfiguration view={view} {...props} />
  ));

  it('disables menu item if `disabled` prop is `true`', async () => {
    const { queryByText, findByText } = render(<SUT disabled />);
    const menuItem = await findByText('Full Screen');

    await userEvent.click(menuItem);

    expect(queryByText('Configuring Full Screen')).toBeNull();
  });

  it('opens modal when menu item is clicked', async () => {
    const { findByText, getByText } = render(<SUT />);
    const menuItem = getByText('Full Screen');

    await userEvent.click(menuItem);

    await findByText('Configuring Full Screen');
  });

  it('shows open modal per default if `open` prop is `true`', () => {
    const { getByText } = render(<SUT show />);

    expect(getByText('Configuring Full Screen')).not.toBeNull();
  });

  it('shows all query titles in modal', () => {
    const viewWithQueries = createViewWithQueries();
    const { getByText } = render(<SUT view={viewWithQueries} show />);

    expect(getByText('Page#1')).not.toBeNull();
    expect(getByText('My awesome Query tab')).not.toBeNull();
    expect(getByText('Page#3')).not.toBeNull();
  });

  it('should not allow strings for the refresh interval', async () => {
    const { getByLabelText } = render(<SUT show />);

    const refreshInterval = asElement(getByLabelText('Refresh Interval'), HTMLInputElement);

    await userEvent.clear(refreshInterval);
    await userEvent.type(refreshInterval, 'a string');

    expect(refreshInterval.value).toBe('');
  });

  it('should not allow strings for the cycle interval', async () => {
    const { getByLabelText } = render(<SUT show />);

    const cycleInterval = asElement(getByLabelText('Tab cycle interval'), HTMLInputElement);

    await userEvent.clear(cycleInterval);
    await userEvent.type(cycleInterval, 'a string');

    expect(cycleInterval.value).toBe('');
  });

  describe('redirects to tv mode page', () => {
    let history;

    beforeEach(() => {
      history = mockHistory();
      asMock(useHistory).mockReturnValue(history);
    });

    it('on form submit', async () => {
      render(<SUT show />);
      const submit = await screen.findByRole('button', { name: /start full screen view/i });

      await userEvent.click(submit);

      expect(history.push).toHaveBeenCalledWith('/dashboards/tv/deadbeef?interval=30&refresh=10');
    });

    it('including changed refresh interval', async () => {
      render(<SUT show />);

      const refreshInterval = screen.getByLabelText('Refresh Interval');

      await userEvent.clear(refreshInterval);
      await userEvent.type(refreshInterval, '42');

      await userEvent.click(await screen.findByRole('button', { name: /start full screen view/i }));

      expect(history.push).toHaveBeenCalledWith('/dashboards/tv/deadbeef?interval=30&refresh=42');
    });

    it('including tab cycle interval setting', async () => {
      render(<SUT show />);

      const cycleInterval = screen.getByLabelText('Tab cycle interval');

      await userEvent.clear(cycleInterval);
      await userEvent.type(cycleInterval, '4242');

      await userEvent.click(await screen.findByRole('button', { name: /start full screen view/i }));

      expect(history.push).toHaveBeenCalledWith('/dashboards/tv/deadbeef?interval=4242&refresh=10');
    });

    it('including selected tabs', async () => {
      const viewWithQueries = createViewWithQueries();
      render(<SUT view={viewWithQueries} show />);

      const query1 = screen.getByLabelText('Page#1');

      await userEvent.click(query1);

      await userEvent.click(await screen.findByRole('button', { name: /start full screen view/i }));

      expect(history.push).toHaveBeenCalledWith('/dashboards/tv/deadbeef?interval=30&refresh=10&tabs=1%2C2');
    });
  });
});
