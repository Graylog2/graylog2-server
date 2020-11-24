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
import { render, fireEvent } from 'wrappedTestingLibrary';
import mockAction from 'helpers/mocking/MockAction';

import { DashboardsActions, DashboardsStoreState } from 'views/stores/DashboardsStore';
import View from 'views/logic/views/View';

import CopyToDashboardForm from './CopyToDashboardForm';

describe('CopyToDashboardForm', () => {
  beforeEach(() => {
    DashboardsActions.search = mockAction(jest.fn(async () => ({
      pagination: {
        total: 0,
        page: 1,
        perPage: 10,
        count: 0,
      },
      list: [],
    })));
  });

  const view1 = View.builder().type(View.Type.Dashboard).id('view-1').title('view 1')
    .build();
  const view2 = View.builder().type(View.Type.Dashboard).id('view-2').title('view 2')
    .build();
  const dashboardList = [view1, view2];
  const dashboardState: DashboardsStoreState = {
    list: dashboardList,
    pagination: {
      total: 2,
      page: 1,
      perPage: 10,
      count: 2,
    },
  };

  it('should render the modal minimal', () => {
    // @ts-ignore
    const { baseElement } = render(<CopyToDashboardForm />);

    expect(baseElement).not.toBeNull();
  });

  it('should render the modal with entries', () => {
    const { baseElement } = render(<CopyToDashboardForm dashboards={dashboardState}
                                                        widgetId="widget-id"
                                                        onCancel={() => {}}
                                                        onSubmit={() => {}} />);

    expect(baseElement).not.toBeNull();
  });

  it('should handle onCancel', () => {
    const onCancel = jest.fn();
    const { getByText } = render(<CopyToDashboardForm dashboards={dashboardState}
                                                      widgetId="widget-id"
                                                      onCancel={onCancel}
                                                      onSubmit={() => {}} />);
    const cancelButton = getByText('Cancel');

    fireEvent.click(cancelButton);

    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('should not handle onSubmit without selection', () => {
    const onSubmit = jest.fn();
    const { getByText } = render(<CopyToDashboardForm dashboards={dashboardState}
                                                      widgetId="widget-id"
                                                      onCancel={() => {}}
                                                      onSubmit={onSubmit} />);
    const submitButton = getByText('Select');

    fireEvent.click(submitButton);

    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('should handle onSubmit with a previous selection', () => {
    const onSubmit = jest.fn();
    const { getByText } = render(<CopyToDashboardForm dashboards={dashboardState}
                                                      widgetId="widget-id"
                                                      onCancel={() => {}}
                                                      onSubmit={onSubmit} />);
    const firstView = getByText('view 1');

    fireEvent.click(firstView);
    const submitButton = getByText('Select');

    fireEvent.click(submitButton);

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith('widget-id', 'view-1');
  });

  it('should query for all dashboards & specific dashboards', () => {
    const { getByPlaceholderText, getByText } = render(
      <CopyToDashboardForm dashboards={dashboardState}
                           widgetId="widget-id"
                           onCancel={() => {}}
                           onSubmit={() => {}} />,
    );

    expect(DashboardsActions.search).toHaveBeenCalledTimes(1);

    const searchInput = getByPlaceholderText('Enter search query...');

    fireEvent.change(searchInput, { target: { value: 'view 1' } });
    const searchButton = getByText('Search');

    fireEvent.click(searchButton);

    expect(DashboardsActions.search).toHaveBeenCalledWith('view 1', 1, 5);
  });
});
