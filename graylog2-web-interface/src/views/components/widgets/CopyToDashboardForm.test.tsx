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
import { render, fireEvent, screen, waitFor } from 'wrappedTestingLibrary';

import View from 'views/logic/views/View';
import Search from 'views/logic/search/Search';
import useDashboards from 'views/components/dashboard/hooks/useDashboards';
import { asMock } from 'helpers/mocking';

import CopyToDashboardForm from './CopyToDashboardForm';

const view1 = View.builder().type(View.Type.Dashboard).id('view-1').title('view 1')
  .search(Search.create())
  .build();
const view2 = View.builder().type(View.Type.Dashboard).id('view-2').title('view 2')
  .search(Search.create())
  .build();
const dashboardList = [view1, view2];

jest.mock('views/components/dashboard/hooks/useDashboards');

describe('CopyToDashboardForm', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useDashboards).mockReturnValue({
      data: {
        list: dashboardList,
        pagination: { total: 2 },
        attributes: [
          {
            id: 'title',
            title: 'Title',
            sortable: true,
          },
          {
            id: 'description',
            title: 'Description',
            sortable: true,
          },
        ],
      },
      isInitialLoading: false,
      refetch: () => {},
    });
  });

  const SUT = (props: Partial<React.ComponentProps<typeof CopyToDashboardForm>>) => (
    <CopyToDashboardForm onCancel={() => {}}
                         onCopyToDashboard={() => Promise.resolve()}
                         onCreateNewDashboard={() => Promise.resolve()}
                         submitButtonText="Submit"
                         submitLoadingText="Submitting..."
                         {...props} />
  );

  const submitModal = () => {
    const submitButton = screen.getByRole('button', { name: /submit/i, hidden: true });
    fireEvent.click(submitButton);
  };

  it('should render the modal minimal', () => {
    asMock(useDashboards).mockReturnValue({
      data: {
        list: [],
        pagination: { total: 0 },
        attributes: [
          {
            id: 'title',
            title: 'Title',
            sortable: true,
          },
          {
            id: 'description',
            title: 'Description',
            sortable: true,
          },
        ],
      },
      isInitialLoading: false,
      refetch: () => {},
    });

    const { baseElement } = render(<SUT />);

    expect(baseElement).not.toBeNull();
  });

  it('should render the modal with entries', () => {
    const { baseElement } = render(<SUT />);

    expect(baseElement).not.toBeNull();
  });

  it('should handle onCancel', () => {
    const onCancel = jest.fn();
    const { getByText } = render(<SUT onCancel={onCancel} />);
    const cancelButton = getByText('Cancel');

    fireEvent.click(cancelButton);

    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('should not handle onSubmit without selection', () => {
    const onSubmit = jest.fn(() => Promise.resolve());

    render(<SUT />);

    submitModal();

    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('should handle onCopyToDashboard with a previous selection', async () => {
    const onSubmit = jest.fn(() => Promise.resolve());
    const { getByText } = render(<SUT onCopyToDashboard={onSubmit} />);
    const firstView = getByText('view 1');

    fireEvent.click(firstView);
    submitModal();

    await screen.findByRole('button', { name: /submit/i, hidden: true });

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith('view-1');
  });

  it('should handle onCreateNewDashboard', async () => {
    const onSubmit = jest.fn(() => Promise.resolve());
    const { findByLabelText } = render(<SUT onCreateNewDashboard={onSubmit} />);

    const checkBox = await findByLabelText(/create a new dashboard/i);

    fireEvent.click(checkBox);
    submitModal();

    await screen.findByRole('button', { name: /submit/i, hidden: true });

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith();
  });

  it('should query for all dashboards & specific dashboards', async () => {
    render(<SUT />);

    expect(useDashboards).toHaveBeenCalledTimes(1);

    const searchInput = screen.getByPlaceholderText('Enter search query...');

    fireEvent.change(searchInput, { target: { value: 'view 1' } });

    await waitFor(() => expect(useDashboards).toHaveBeenCalledWith({
      query: 'view 1',
      page: 1,
      pageSize: 5,
      sort: {
        attributeId: 'title',
        direction: 'asc',
      },
    }));
  });
});
