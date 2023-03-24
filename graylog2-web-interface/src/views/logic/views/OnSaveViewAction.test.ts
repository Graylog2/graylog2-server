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
import mockDispatch from 'views/test/mockDispatch';
import type { RootState } from 'views/types';
import OnSaveViewAction from 'views/logic/views/OnSaveViewAction';
import { setIsDirty, setIsNew } from 'views/logic/slices/viewSlice';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import UserNotification from 'util/UserNotification';
import { asMock } from 'helpers/mocking';

import View from './View';

jest.mock('routing/Routes', () => ({ VIEWS: { VIEWID: (viewId: string) => `/views/${viewId}` } }));

jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    update: jest.fn((view: View) => Promise.resolve(view)),
  },
}));

jest.mock('util/UserNotification');

describe('OnSaveViewAction', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('saves a given view', async () => {
    const view = View.create();
    const dispatch = mockDispatch({ view: { view } } as RootState);

    await dispatch(OnSaveViewAction(view));

    expect(dispatch).toHaveBeenCalledWith(setIsDirty(false));
    expect(dispatch).toHaveBeenCalledWith(setIsNew(false));
    expect(ViewManagementActions.update).toHaveBeenCalledWith(view);
  });

  it('shows notification upon success', async () => {
    const view = View.create().toBuilder().title('Sample view').build();
    const dispatch = mockDispatch({ view: { view } } as RootState);

    await dispatch(OnSaveViewAction(view));

    expect(UserNotification.success).toHaveBeenCalledWith('Saving view "Sample view" was successful!', 'Success!');
  });

  it('does not do anything if saving fails', async () => {
    asMock(ViewManagementActions.update).mockRejectedValue(new Error('Something bad happened!'));
    const view = View.create();
    const dispatch = mockDispatch({ view: { view } } as RootState);

    await dispatch(OnSaveViewAction(view));

    expect(UserNotification.success).not.toHaveBeenCalled();
    expect(UserNotification.error).toHaveBeenCalledWith('Saving view failed: Error: Something bad happened!', 'Error!');
  });
});
