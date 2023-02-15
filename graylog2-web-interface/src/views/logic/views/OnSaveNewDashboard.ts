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
import UserNotification from 'util/UserNotification';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import type { AppDispatch } from 'stores/useAppDispatch';
import { loadView, setIsDirty, setIsNew } from 'views/logic/slices/viewSlice';
import { loadDashboard } from 'views/logic/views/Actions';

import type View from './View';

export default (view: View) => async (dispatch: AppDispatch) => {
  try {
    const savedView = await ViewManagementActions.create(view);
    dispatch(loadView(savedView));
    dispatch(setIsDirty(false));
    dispatch(setIsNew(false));
    loadDashboard(savedView.id);
    UserNotification.success(`Saving view "${view.title}" was successful!`, 'Success!');
  } catch (error) {
    UserNotification.error(`Saving view failed: ${error}`, 'Error!');
  }
};
