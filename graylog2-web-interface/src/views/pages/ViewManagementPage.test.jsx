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
import React from 'react';
import { mount, shallow } from 'wrappedEnzyme';
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
    const ViewManagementPage = require('./ViewManagementPage').default;
    const wrapper = shallow(<ViewManagementPage />);

    const viewList = wrapper.find('view-list');

    expect(viewList).toHaveLength(1);
    expect(viewList.at(0)).toHaveProp('views', viewsResult.list);
    expect(viewList.at(0)).toHaveProp('pagination', viewsResult.pagination);
  });

  it('asks for confirmation when deleting view', () => {
    // eslint-disable-next-line global-require
    const ViewManagementPage = require('./ViewManagementPage').default;
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
