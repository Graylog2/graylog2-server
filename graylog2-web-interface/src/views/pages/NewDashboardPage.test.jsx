// @flow strict
import * as React from 'react';
import { mount } from 'enzyme';

import mockAction from 'helpers/mocking/MockAction';
import mockComponent from 'helpers/mocking/MockComponent';
import { ViewActions } from 'views/stores/ViewStore';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import NewDashboardPage from './NewDashboardPage';

jest.mock('./ExtendedSearchPage', () => mockComponent('ExtendedSearchPage'));

describe('NewDashboardPage', () => {
  it('should render minimal', () => {
    const wrapper = mount(<NewDashboardPage route={{}} location={{}} />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render transform search view to dashboard view', () => {
    const view = View.create().toBuilder().type(View.Type.Search).search(Search.builder().build())
      .createdAt(new Date('2019-10-16T14:38:44.681Z'))
      .build();
    ViewActions.load = mockAction(jest.fn(() => new Promise(() => {})));
    const wrapper = mount(<NewDashboardPage route={{}}
                                            location={{ state: { view } }} />);
    expect(wrapper).toMatchSnapshot();
    expect(ViewActions.load).toHaveBeenCalledTimes(1);
    expect(ViewActions.load.mock.calls[0][0].type).toStrictEqual(View.Type.Dashboard);
  });
});
