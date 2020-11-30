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
// @flow strict
import asMock from 'helpers/mocking/AsMock';

import { ViewActions } from 'views/stores/ViewStore';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import UserNotification from 'util/UserNotification';

import View from './View';
import OnSaveAsViewAction from './OnSaveAsViewAction';
import { loadDashboard } from './Actions';

jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementActions: {
    create: jest.fn((v) => Promise.resolve(v)).mockName('create'),
  },
}));

jest.mock('views/stores/ViewStore', () => ({
  ViewActions: {
    load: jest.fn(() => Promise.resolve({ view: { id: 'deadbeef' } })).mockName('load'),
  },
}));

jest.mock('util/UserNotification');
jest.mock('views/logic/views/Actions');

describe('OnSaveAsViewAction', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('saves a given new view', async () => {
    const view = View.create();

    await OnSaveAsViewAction(view);

    expect(ViewManagementActions.create).toHaveBeenCalled();
    expect(ViewManagementActions.create).toHaveBeenCalledWith(view);
  });

  it('loads saved view', async () => {
    const view = View.create();

    await OnSaveAsViewAction(view);

    expect(ViewActions.load).toHaveBeenCalled();
    expect(ViewActions.load).toHaveBeenCalledWith(view);
  });

  it('redirects to saved view', async () => {
    const view = View.create();

    await OnSaveAsViewAction(view);

    expect(loadDashboard).toHaveBeenCalledWith('deadbeef');
  });

  it('shows notification upon success', async () => {
    const view = View.create().toBuilder().title('Test View').build();

    await OnSaveAsViewAction(view);

    expect(UserNotification.success).toHaveBeenCalled();
    expect(UserNotification.success).toHaveBeenCalledWith(`Saving view "${view.title}" was successful!`, 'Success!');
  });

  it('does not do anything if saving fails', async () => {
    asMock(ViewManagementActions.create).mockImplementation(() => Promise.reject(new Error('Something bad happened!')));

    const view = View.create();

    await OnSaveAsViewAction(view);

    expect(ViewActions.load).not.toHaveBeenCalled();
    expect(loadDashboard).not.toHaveBeenCalled();
    expect(UserNotification.success).not.toHaveBeenCalled();
    expect(UserNotification.error).toHaveBeenCalledTimes(1);
    expect(UserNotification.error).toHaveBeenCalledWith('Saving view failed: Error: Something bad happened!', 'Error!');
  });
});
