import React from 'react';
import { shallow } from 'enzyme';
import { StoreMock } from 'helpers/mocking';

import ViewList from './components/views/ViewList';

jest.mock('routing/Routes', () => ({ pluginRoute: x => x }));

const mockViewManagementStore = StoreMock('listen', 'getInitialState');
const mockViewManagementActions = { search: jest.fn(), delete: jest.fn() };
jest.mock('enterprise/stores/ViewManagementStore', () => ({ ViewManagementStore: mockViewManagementStore, ViewManagementActions: mockViewManagementActions }));

describe('ViewManagementPage', () => {
  it('passes retrieved views to list component', () => {
    const viewsResult = {
      list: [{ title: 'A View' }],
      pagination: {
        total: 42,
        page: 1,
        perPage: 1,
      },
    };

    mockViewManagementStore.getInitialState.mockImplementationOnce(() => viewsResult);

    // eslint-disable-next-line global-require
    const ViewManagementPage = require('enterprise/ViewManagementPage').default;
    const wrapper = shallow(<ViewManagementPage />);

    const viewList = wrapper.find(ViewList);
    expect(viewList).toHaveLength(1);
    expect(viewList.at(0)).toHaveProp('views', viewsResult.list);
    expect(viewList.at(0)).toHaveProp('pagination', viewsResult.pagination);
  });
});
