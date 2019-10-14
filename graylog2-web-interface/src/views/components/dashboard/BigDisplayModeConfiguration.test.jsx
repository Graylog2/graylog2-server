// @flow strict
import * as React from 'react';
import { render, cleanup, fireEvent, waitForElement } from '@testing-library/react';

import history from 'util/History';
import Routes from 'routing/Routes';
import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import BigDisplayModeConfiguration from './BigDisplayModeConfiguration';
import Query from '../../logic/queries/Query';

jest.mock('util/History', () => ({}));
jest.mock('routing/Routes', () => ({ pluginRoute: jest.fn() }));

describe('BigDisplayModeConfiguration', () => {
  afterEach(cleanup);

  const search = Search.create();
  const view = View.create()
    .toBuilder()
    .id('deadbeef')
    .type(View.Type.Dashboard)
    .search(search)
    .build();

  it('generates markup that matches snapshot', () => {
    const { container } = render(<BigDisplayModeConfiguration view={view} />);
    expect(container).toMatchSnapshot();
  });

  it('opens modal when menu item is clicked', async () => {
    const { getByText } = render(<BigDisplayModeConfiguration view={view} />);
    const menuItem = getByText('Full Screen');
    fireEvent.click(menuItem);

    await waitForElement(() => getByText('Configuring Full Screen'));
  });

  it('shows open modal per default if `open` prop is `true`', () => {
    const { getByText } = render(<BigDisplayModeConfiguration view={view} open />);

    expect(getByText('Configuring Full Screen')).not.toBeNull();
  });

  it('shows all query titles in modal', () => {
    const queries = [
      Query.builder().build(),
      Query.builder().build(),
    ];
    const searchWithQueries = search.toBuilder()
      .queries(queries)
      .build();
    const viewWithQueries = view.toBuilder().search(searchWithQueries).build();
    const { getByText } = render(<BigDisplayModeConfiguration view={viewWithQueries} open />);

    expect(getByText('Query#1')).not.toBeNull();
    expect(getByText('Query#2')).not.toBeNull();
  });

  it('redirects to tv mode page when "Save" is clicked', () => {
    const { getByText } = render(<BigDisplayModeConfiguration view={view} open />);
    const saveButton = getByText('Save');
    expect(saveButton).not.toBeNull();
    history.push = jest.fn();
    Routes.pluginRoute = jest.fn(() => viewId => `/dashboards/tv/${viewId}`);

    fireEvent.click(saveButton);

    expect(Routes.pluginRoute).toHaveBeenCalledWith('DASHBOARDS_TV_VIEWID');
    expect(history.push).toHaveBeenCalledWith('/dashboards/tv/deadbeef?cycle=false&refresh=10');
  });
});
