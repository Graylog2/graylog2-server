import React from 'react';
import { mountWithTheme as mount, shallowWithTheme as shallow } from 'theme/enzymeWithTheme';
import { StoreMock } from 'helpers/mocking';
import mockComponent from 'helpers/mocking/MockComponent';

jest.mock('routing/Routes', () => ({
  VIEWS: '/views',
  EXTENDEDSEARCH: '/extendedsearch',
}));
jest.mock('views/components/views/ViewList', () => 'view-list');
jest.mock('components/common', () => ({
  DocumentTitle: mockComponent('DocumentTitle'),
  PageHeader: mockComponent('PageHeader'),
}));

const mockViewManagementStore = StoreMock('listen', 'getInitialState');
const mockViewManagementActions = { search: jest.fn(), delete: jest.fn() };
jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementStore: mockViewManagementStore,
  ViewManagementActions: mockViewManagementActions,
}));

describe('ViewManagementPage', () => {
  const viewsResult = {
    list: [{ title: 'A View' }],
    pagination: {
      total: 42,
      page: 1,
      perPage: 1,
    },
  };
  beforeEach(() => {
    mockViewManagementStore.getInitialState.mockImplementationOnce(() => viewsResult);
  });
  it('passes retrieved views to list component', () => {
    // eslint-disable-next-line global-require
    const ViewManagementPage = require('./ViewManagementPage');
    const wrapper = shallow(<ViewManagementPage />);

    const viewList = wrapper.find('view-list');
    expect(viewList).toHaveLength(1);
    expect(viewList.at(0)).toHaveProp('views', viewsResult.list);
    expect(viewList.at(0)).toHaveProp('pagination', viewsResult.pagination);
  });
  it('asks for confirmation when deleting view', () => {
    // eslint-disable-next-line global-require
    const ViewManagementPage = require('./ViewManagementPage');
    const wrapper = mount(<ViewManagementPage />);

    const viewList = wrapper.find('view-list');
    const { handleViewDelete } = viewList.at(0).props();

    const oldConfirm = window.confirm;
    const dummyView = { title: 'Dummy view' };

    window.confirm = jest.fn(() => false);

    handleViewDelete(dummyView);

    expect(window.confirm).toHaveBeenCalledWith('Are you sure you want to delete "Dummy view"?');
    expect(mockViewManagementActions.delete).not.toHaveBeenCalled();

    window.confirm = jest.fn(() => true);

    handleViewDelete(dummyView);

    expect(mockViewManagementActions.delete).toHaveBeenCalledWith(dummyView);

    window.confirm = oldConfirm;
  });
});
