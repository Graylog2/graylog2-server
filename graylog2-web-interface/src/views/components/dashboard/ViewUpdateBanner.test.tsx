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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { asMock } from 'helpers/mocking';
import useViewsSelector from 'views/stores/useViewsSelector';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import { getView } from 'views/api/views';
import ViewDeserializer from 'views/logic/views/ViewDeserializer';
import useView from 'views/hooks/useView';
import View from 'views/logic/views/View';
import type { ViewJson } from 'views/logic/views/View';

import ViewUpdateBanner from './ViewUpdateBanner';

jest.mock('views/stores/useViewsSelector');
jest.mock('views/stores/useViewsDispatch');
jest.mock('views/hooks/useView');
jest.mock('views/api/views');
jest.mock('views/logic/views/ViewDeserializer');

describe('ViewUpdateBanner', () => {
  const mockDispatch = jest.fn();
  const mockView = View.create().toBuilder().id('view-id-1').build();

  beforeEach(() => {
    asMock(useViewsDispatch).mockReturnValue(mockDispatch);
    asMock(useView).mockReturnValue(mockView);
    asMock(useViewsSelector).mockReturnValue(undefined);
  });

  it('renders nothing when serverViewLastUpdatedAt is undefined', () => {
    render(<ViewUpdateBanner />);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('shows banner when serverViewLastUpdatedAt is set', () => {
    asMock(useViewsSelector).mockReturnValue('2024-01-01T12:00:00.000Z');
    render(<ViewUpdateBanner />);
    expect(screen.getByText(/updated by another user/i)).toBeInTheDocument();
  });

  it('dispatches setServerViewLastUpdatedAt(undefined) when dismissed', async () => {
    asMock(useViewsSelector).mockReturnValue('2024-01-01T12:00:00.000Z');
    render(<ViewUpdateBanner />);
    await userEvent.click(screen.getByRole('button', { name: /close/i }));
    expect(mockDispatch).toHaveBeenCalledWith(expect.objectContaining({ payload: undefined }));
  });

  it('calls getView and dispatches loadView when Reload is clicked', async () => {
    asMock(useViewsSelector).mockReturnValue('2024-01-01T12:00:00.000Z');
    const freshViewJson = { id: mockView.id } as unknown as ViewJson;
    const freshView = View.create().toBuilder().id('view-id-1').build();
    asMock(getView).mockResolvedValue(freshViewJson);
    asMock(ViewDeserializer).mockResolvedValue(freshView);
    mockDispatch.mockResolvedValue(undefined);

    render(<ViewUpdateBanner />);
    await userEvent.click(screen.getByRole('button', { name: /reload/i }));

    await waitFor(() => expect(getView).toHaveBeenCalledWith(mockView.id));
    expect(ViewDeserializer).toHaveBeenCalledWith(freshViewJson);
    expect(mockDispatch).toHaveBeenCalledWith(expect.any(Function));
  });
});
