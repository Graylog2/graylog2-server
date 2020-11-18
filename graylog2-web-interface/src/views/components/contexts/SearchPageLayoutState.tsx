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
import * as React from 'react';
import { useContext, useState } from 'react';

import View, { ViewType } from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import UserPreferencesContext from 'contexts/UserPreferencesContext';
import CurrentUserContext from 'contexts/CurrentUserContext';
import CombinedProvider from 'injection/CombinedProvider';
import Store from 'logic/local-storage/Store';

const { PreferencesActions } = CombinedProvider.get('Preferences');

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

const _userSidebarPinningPref = (currentUser, userPreferences, viewType) => {
  const sidebarPinningPrefKey = _getPinningPreferenceKey(viewType);

  // eslint-disable-next-line camelcase
  if (currentUser?.read_only) {
    return Store.get(sidebarPinningPrefKey);
  }

  return userPreferences[sidebarPinningPrefKey];
};

const _updateUserSidebarPinningPref = (currentUser, userPreferences, viewType, newIsPinned) => {
  const sidebarPinningPrefKey: string = _getPinningPreferenceKey(viewType);

  // eslint-disable-next-line camelcase
  if (currentUser?.read_only) {
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

  const _onSidebarPinningChange = (newIsPinned) => _updateUserSidebarPinningPref(currentUser, userPreferences, viewType, newIsPinned);

  const getLayoutState = (stateKey: string, defaultValue: boolean) => {
    return state[stateKey] ?? defaultValue;
  };

  const setLayoutState = (stateKey: string, value: boolean) => {
    if (stateKey === 'sidebarIsPinned') {
      _onSidebarPinningChange(value);
    }

    setState({ ...state, [stateKey]: value });
  };

  return children({ getLayoutState, setLayoutState });
};

export default SearchPageLayoutState;
