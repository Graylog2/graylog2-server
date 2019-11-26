// @flow strict
import * as React from 'react';
import { render, cleanup, fireEvent } from '@testing-library/react';
import { DashboardsActions } from 'views/stores/DashboardsStore';
import mockAction from 'helpers/mocking/MockAction';

import View from 'views/logic/views/View';
import CopyToDashboardForm from './CopyToDashboardForm';

describe('CopyToDashboardForm', () => {
  afterEach(cleanup);

  const view1 = View.builder().type(View.Type.Dashboard).id('view-1').title('view 1')
    .build();
  const view2 = View.builder().type(View.Type.Dashboard).id('view-2').title('view 2')
    .build();
  const dashboardList = [view1, view2];
  const dashboardState = {
    list: dashboardList,
    pagination: {
      total: 2,
      page: 1,
      per_page: 10,
      count: 2,
    },
  };

  it('should render the modal minimal', () => {
    const { baseElement } = render(<CopyToDashboardForm />);
    expect(baseElement).toMatchSnapshot();
  });

  it('should render the modal with entries', () => {
    const { baseElement } = render(<CopyToDashboardForm dashboards={dashboardState}
                                                        widgetId="widget-id"
                                                        onCancel={() => {}}
                                                        onSubmit={() => {}} />);
    expect(baseElement).toMatchSnapshot();
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

  it('should handle search of dashboards', () => {
    DashboardsActions.search = mockAction(jest.fn(() => Promise.resolve()));
    const { getByPlaceholderText, getByText } = render(<CopyToDashboardForm dashboards={dashboardState}
                                                                            widgetId="widget-id"
                                                                            onCancel={() => {}}
                                                                            onSubmit={() => {}} />);
    const searchInput = getByPlaceholderText('Enter search query...');
    fireEvent.change(searchInput, { target: { value: 'view 1' } });
    const searchButton = getByText('Search');
    fireEvent.click(searchButton);
    expect(DashboardsActions.search).toHaveBeenCalledTimes(1);
  });
});
