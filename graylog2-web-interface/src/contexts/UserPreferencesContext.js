// @flow strict
import * as React from 'react';

import { singleton } from '../views/logic/singleton';

export type UserPreferences = {
  enableSmartSearch: boolean,
  updateUnfocussed: boolean,
  searchSidebarIsPinned: boolean,
};

export const defaultUserPreferences = {
  enableSmartSearch: true,
  updateUnfocussed: false,
  searchSidebarIsPinned: false,
};

const UserPreferencesContext = React.createContext<UserPreferences>(defaultUserPreferences);
export default singleton('contexts.UserPreferencesContext', () => UserPreferencesContext);
