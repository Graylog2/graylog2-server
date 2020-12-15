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

import { DEFAULT_THEME_MODE, PREFERENCES_THEME_MODE, ThemeMode } from 'theme/constants';

import { singleton } from '../views/logic/singleton';

export type UserPreferences = {
  enableSmartSearch: boolean,
  updateUnfocussed: boolean,
  searchSidebarIsPinned?: boolean,
  dashboardSidebarIsPinned?: boolean,
  [PREFERENCES_THEME_MODE]: ThemeMode,
};

export const defaultUserPreferences = {
  enableSmartSearch: true,
  updateUnfocussed: false,
  searchSidebarIsPinned: false,
  dashboardSidebarIsPinned: false,
  [PREFERENCES_THEME_MODE]: DEFAULT_THEME_MODE,
};

const UserPreferencesContext = React.createContext<UserPreferences>(defaultUserPreferences);
export default singleton('contexts.UserPreferencesContext', () => UserPreferencesContext);
