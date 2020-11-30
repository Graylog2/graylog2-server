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
import UserNotification from 'util/UserNotification';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import { ViewActions } from 'views/stores/ViewStore';

import View from './View';
import { loadDashboard } from './Actions';

export default (view: View) => {
  return ViewManagementActions.create(view)
    .then(() => ViewActions.load(view))
    .then((state) => loadDashboard(state.view.id))
    .then(() => UserNotification.success(`Saving view "${view.title}" was successful!`, 'Success!'))
    .catch((error) => UserNotification.error(`Saving view failed: ${error}`, 'Error!'));
};
