import React from 'react';
import { shallow } from 'enzyme';
import { StoreMock } from 'helpers/mocking';

jest.mock('routing/Routes', () => ({ pluginRoute: x => x }));
jest.mock('enterprise/components/views/ViewList', () => 'view-list');
jest.mock('components/common', () => ({
  /* eslint-disable global-require */
  DocumentTitle: require('components/common/DocumentTitle').default,
  PageHeader: require('components/common/PageHeader').default,
  /* eslint-enable global-require */
}));

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
    const ViewManagementPage = require('./ViewManagementPage').default;
    const wrapper = shallow(<ViewManagementPage />);

    const viewList = wrapper.find('view-list');
    expect(viewList).toHaveLength(1);
    expect(viewList.at(0)).toHaveProp('views', viewsResult.list);
    expect(viewList.at(0)).toHaveProp('pagination', viewsResult.pagination);
  });
});
