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
import asMock from 'helpers/mocking/AsMock';
import UserNotification from 'util/UserNotification';
import mockDispatch from 'views/test/mockDispatch';
import type { RootState } from 'views/types';
import type { HistoryFunction } from 'routing/useHistory';
import { setIsDirty, setIsNew } from 'views/logic/slices/viewSlice';
import { createEntityShareState } from 'fixtures/entityShareState';
import { createView } from 'views/api/views';
import useEntityShareState from 'hooks/useEntityShareState';

import View from './View';
import OriginalOnSaveNewDashboard from './OnSaveNewDashboard';
import { loadDashboardWithSearch } from './Actions';

jest.mock('views/api/views', () => ({
  createView: jest.fn((v) => Promise.resolve(v)).mockName('create'),
}));
jest.mock('api/entity-share');
jest.mock('hooks/useEntityShareState');

jest.mock('util/UserNotification');
jest.mock('views/logic/views/Actions');

const history: HistoryFunction = {
  goBack: jest.fn(),
  push: jest.fn(),
  pushWithState: jest.fn(),
  replace: jest.fn(),
};

const OnSaveNewDashboard = (view: View) => OriginalOnSaveNewDashboard(view, history);

describe('OnSaveNewDashboard', () => {
  beforeEach(() => {
    asMock(useEntityShareState).mockReturnValue({ data: createEntityShareState });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('saves a given new view', async () => {
    const view = View.create();
    const dispatch = mockDispatch({ view: { view } } as RootState);

    await dispatch(OnSaveNewDashboard(view));

    expect(createView).toHaveBeenCalled();
    expect(createView).toHaveBeenCalledWith(view, undefined, undefined);
  });

  it('loads saved view', async () => {
    const view = View.create();
    const dispatch = mockDispatch({ view: { view } } as RootState);

    await dispatch(OnSaveNewDashboard(view));

    expect(loadDashboardWithSearch).toHaveBeenCalled();
  });

  it('sets dirty flag to false', async () => {
    const view = View.create();
    const dispatch = mockDispatch({ view: { view } } as RootState);

    await dispatch(OnSaveNewDashboard(view));

    expect(dispatch).toHaveBeenCalledWith(setIsDirty(false));
  });

  it('sets new flag to false', async () => {
    const view = View.create();
    const dispatch = mockDispatch({ view: { view } } as RootState);

    await dispatch(OnSaveNewDashboard(view));

    expect(dispatch).toHaveBeenCalledWith(setIsNew(false));
  });

  it('redirects to saved view', async () => {
    const view = View.create().toBuilder().id('deadbeef').build();
    const dispatch = mockDispatch({ view: { view } } as RootState);

    await dispatch(OnSaveNewDashboard(view));

    expect(loadDashboardWithSearch).toHaveBeenCalledWith(expect.anything(), 'deadbeef', undefined);
  });

  it('keeps query parameters when redirecting to saved view', async () => {
    const view = View.create().toBuilder().id('deadbeef').build();
    const dispatch = mockDispatch({ view: { view } } as RootState);
    const search = '?page=query-id&rangetype=relative&relative=3600';

    await dispatch(OriginalOnSaveNewDashboard(view, history, undefined, 'existing-id', search));

    expect(loadDashboardWithSearch).toHaveBeenCalledWith(expect.anything(), 'deadbeef', search);
  });

  it('shows notification upon success', async () => {
    const view = View.create().toBuilder().title('Test View').build();
    const dispatch = mockDispatch({ view: { view } } as RootState);

    await dispatch(OnSaveNewDashboard(view));

    expect(UserNotification.success).toHaveBeenCalled();
    expect(UserNotification.success).toHaveBeenCalledWith(`Saving view "${view.title}" was successful!`, 'Success!');
  });

  it('does not do anything if saving fails', async () => {
    asMock(createView).mockImplementation(() => Promise.reject(new Error('Something bad happened!')));

    const view = View.create();
    const dispatch = mockDispatch({ view: { view } } as RootState);

    await dispatch(OnSaveNewDashboard(view));

    expect(loadDashboardWithSearch).not.toHaveBeenCalled();
    expect(UserNotification.success).not.toHaveBeenCalled();
    expect(UserNotification.error).toHaveBeenCalledTimes(1);
    expect(UserNotification.error).toHaveBeenCalledWith('Saving view failed: Error: Something bad happened!', 'Error!');
  });
});
