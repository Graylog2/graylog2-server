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
import type * as React from 'react';
import { useContext, useState, useCallback } from 'react';

import type { ViewType } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import type { UserPreferences } from 'contexts/UserPreferencesContext';
import UserPreferencesContext from 'contexts/UserPreferencesContext';
import CurrentUserContext from 'contexts/CurrentUserContext';
import Store from 'logic/local-storage/Store';
import { PreferencesActions } from 'stores/users/PreferencesStore';
import type User from 'logic/users/User';

type Props = {
  children: (layoutConsumer: {
    setLayoutState: (stateKey: string, value: boolean) => void,
    getLayoutState: (stateKey: string, defaultValue: boolean) => boolean,
  }) => React.ReactElement;
};

type PinningPreferenceKey = 'dashboardSidebarIsPinned' | 'searchSidebarIsPinned';
const preferenceKeyMapping: { [key in typeof View.Type.Dashboard | typeof View.Type.Search]: PinningPreferenceKey } = {
  [View.Type.Dashboard]: 'dashboardSidebarIsPinned',
  [View.Type.Search]: 'searchSidebarIsPinned',
};

const _getPinningPreferenceKey = (viewType: ViewType | undefined): PinningPreferenceKey => {
  const preferenceKey = viewType && preferenceKeyMapping[viewType];

  if (!preferenceKey) {
    throw new Error(`User sidebar pinning preference key is missing for view type ${viewType ?? '(type not provided)'}`);
  }

  return preferenceKey;
};

const _userSidebarPinningPref = (currentUser: User, userPreferences: UserPreferences, viewType: ViewType | undefined) => {
  const sidebarPinningPrefKey = _getPinningPreferenceKey(viewType);

  if (currentUser?.readOnly) {
    return Store.get(sidebarPinningPrefKey);
  }

  return userPreferences[sidebarPinningPrefKey];
};

const _updateUserSidebarPinningPref = (currentUser: User, userPreferences: UserPreferences, viewType: ViewType | undefined, newIsPinned: boolean) => {
  const sidebarPinningPrefKey: string = _getPinningPreferenceKey(viewType);

  if (currentUser?.readOnly) {
    Store.set(sidebarPinningPrefKey, newIsPinned);
  } else {
    const newUserPreferences = {
      ...userPreferences,
      [sidebarPinningPrefKey]: newIsPinned,
    };
    PreferencesActions.saveUserPreferences(currentUser?.username, newUserPreferences, undefined, false);
  }
};

const SearchPageLayoutState = ({ children }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  const userPreferences = useContext(UserPreferencesContext);
  const viewType = useContext(ViewTypeContext);
  const [state, setState] = useState({
    sidebarIsPinned: _userSidebarPinningPref(currentUser, userPreferences, viewType),
  });

  const _onSidebarPinningChange = useCallback((newIsPinned: boolean) => _updateUserSidebarPinningPref(currentUser, userPreferences, viewType, newIsPinned), [currentUser, userPreferences, viewType]);

  const getLayoutState = useCallback((stateKey: string, defaultValue: boolean) => {
    return state[stateKey] ?? defaultValue;
  }, [state]);

  const setLayoutState = useCallback((stateKey: string, value: boolean) => {
    if (stateKey === 'sidebarIsPinned') {
      _onSidebarPinningChange(value);
    }

    setState({ ...state, [stateKey]: value });
  }, [_onSidebarPinningChange, state]);

  return children({ getLayoutState, setLayoutState });
};

export default SearchPageLayoutState;
