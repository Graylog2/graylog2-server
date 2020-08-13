// @flow strict
import * as React from 'react';

import { PREFERENCES_THEME_MODE, DEFAULT_THEME_MODE } from 'theme/constants';

import { singleton } from '../views/logic/singleton';

export type UserPreferences = {
  enableSmartSearch: boolean,
  updateUnfocussed: boolean,
  searchSidebarIsPinned: boolean,
  dashboardSidebarIsPinned: boolean,
  [PREFERENCES_THEME_MODE]: string,
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
