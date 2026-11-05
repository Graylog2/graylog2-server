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
import i18n from 'i18n';
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
    UserNotification.success(
      i18n.t('actions_menu.save_success', { ns: 'search', title: view.title }),
      i18n.t('save_view_notifications.success_title', { ns: 'search' }),
    );
  } catch (error) {
    UserNotification.error(
      i18n.t('actions_menu.save_error', { ns: 'search', error: _extractErrorMessage(error) }),
      i18n.t('save_view_notifications.error_title', { ns: 'search' }),
    );
  }
};
