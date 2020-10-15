// @flow strict
import * as React from 'react';

import { DEFAULT_THEME_MODE, PREFERENCES_THEME_MODE, type PreferencesThemeMode, type ThemeMode } from 'theme/constants';

import { singleton } from '../views/logic/singleton';

export type UserPreferences = {
  enableSmartSearch: boolean,
  updateUnfocussed: boolean,
  searchSidebarIsPinned?: boolean,
  dashboardSidebarIsPinned?: boolean,
  [PreferencesThemeMode]: ThemeMode,
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
