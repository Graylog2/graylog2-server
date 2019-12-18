// @flow strict
import * as React from 'react';
import { render, cleanup, waitForElement } from 'wrappedTestingLibrary';

import mockAction from 'helpers/mocking/MockAction';
import { ViewActions } from 'views/stores/ViewStore';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import NewDashboardPage from './NewDashboardPage';

jest.mock('./ExtendedSearchPage', () => () => <div>Extended search page</div>);
jest.mock('views/stores/ViewStore', () => ({ ViewActions: {} }));
jest.mock('views/logic/views/ViewLoader', () => ({
  processHooks: jest.fn((promise, loadHooks, executeHooks, query, onSuccess) => Promise.resolve().then(onSuccess)),
}));

describe('NewDashboardPage', () => {
  afterEach(cleanup);
  it('should render minimal', async () => {
    ViewActions.create = mockAction(jest.fn(() => Promise.resolve()));

    const { getByText } = render(<NewDashboardPage route={{}} location={{}} />);

    await waitForElement(() => getByText('Extended search page'));
  });

  it('should render transform search view to dashboard view', async () => {
    const view = View.create().toBuilder().type(View.Type.Search).search(Search.builder().build())
      .createdAt(new Date('2019-10-16T14:38:44.681Z'))
      .build();
    ViewActions.load = mockAction(jest.fn(() => new Promise(() => {})));

    const { getByText } = render((
      <NewDashboardPage route={{}}
                        location={{ state: { view } }} />
    ));

    await waitForElement(() => getByText('Extended search page'));
    expect(ViewActions.load).toHaveBeenCalledTimes(1);
    /* $FlowFixMe ViewActions.load was overridden to test the call */
    expect(ViewActions.load.mock.calls[0][0].type).toStrictEqual(View.Type.Dashboard);
  });

  it('should not render transform search view to dashboard view if view search is in JSON format', async () => {
    const view = View.create().toBuilder().type(View.Type.Search).search(Search.builder().build())
      .createdAt(new Date('2019-10-16T14:38:44.681Z'))
      .build()
      .toJSON();
    ViewActions.load = mockAction(jest.fn(() => new Promise(() => {})));

    const { getByText } = render((
      <NewDashboardPage route={{}}
                        location={{ state: { view } }} />
    ));

    await waitForElement(() => getByText('Extended search page'));
    expect(ViewActions.load).toHaveBeenCalledTimes(0);
  });
});
