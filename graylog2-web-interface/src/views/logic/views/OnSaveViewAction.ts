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
import type View from 'views/logic/views/View';
import type { ViewsDispatch } from 'views/stores/useViewsDispatch';
import { setIsNew, setIsDirty } from 'views/logic/slices/viewSlice';
import type FetchError from 'logic/errors/FetchError';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';
import { updateView } from 'views/api/views';

const _extractErrorMessage = (error: FetchError) =>
  error && error.additional && error.additional.body && error.additional.body.message
    ? error.additional.body.message
    : error;

export default (view: View, entityShare?: EntitySharePayload) => async (dispatch: ViewsDispatch) => {
  try {
    await updateView(view, entityShare);
    dispatch(setIsNew(false));
    dispatch(setIsDirty(false));
    UserNotification.success(`Saving view "${view.title}" was successful!`, 'Success!');
  } catch (error) {
    UserNotification.error(`Saving view failed: ${_extractErrorMessage(error)}`, 'Error!');
  }
};
