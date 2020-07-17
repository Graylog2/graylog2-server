// @flow strict
import * as React from 'react';
import { useContext, useState } from 'react';

import View, { type ViewType } from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import UserPreferencesContext from 'contexts/UserPreferencesContext';
import CurrentUserContext from 'contexts/CurrentUserContext';
import CombinedProvider from 'injection/CombinedProvider';
import Store from 'logic/local-storage/Store';

const { PreferencesActions } = CombinedProvider.get('Preferences');

type Props = {
  children: ({
    setLayoutState: (stateKey: string, value: boolean) => void,
    getLayoutState: (stateKey: string) => boolean,
  }) => React.Node,
};

const _getPinningPreferenceKey = (viewType: ?ViewType): string => {
  const preferenceKeyMapping = {
    [View.Type.Dashboard]: 'dashboardSidebarIsPinned',
    [View.Type.Search]: 'searchSidebarIsPinned',
  };

  const preferenceKey = viewType && preferenceKeyMapping[viewType];

  if (!preferenceKey) {
    throw new Error(`User sidebar pinning preference key is missing for view type ${viewType ?? '(type not provided)'}`);
  }

  return preferenceKey;
};

const _defaultSidebarPinning = (currentUser, userPreferences, viewType) => {
  const fallbackDefault = false;
  const sidebarPinningPrefKey = _getPinningPreferenceKey(viewType);

  // eslint-disable-next-line camelcase
  if (currentUser?.read_only) {
    return Store.get(sidebarPinningPrefKey) ?? fallbackDefault;
  }

  return userPreferences[sidebarPinningPrefKey] ?? fallbackDefault;
};

const _createUserPreferencesArray = (userPreferences) => {
  return Object.entries(userPreferences).map(([name, value]) => ({
    name,
    value,
  }));
};

const _updateUserSidebarPinning = (currentUser, userPreferences, viewType, newIsPinned) => {
  const sidebarPinningPrefKey = _getPinningPreferenceKey(viewType);

  // eslint-disable-next-line camelcase
  if (currentUser?.read_only) {
    Store.set(sidebarPinningPrefKey, newIsPinned);
  } else {
    const newUserPreferences = {
      ...userPreferences,
      [sidebarPinningPrefKey]: newIsPinned,
    };
    PreferencesActions.saveUserPreferences(currentUser?.username, _createUserPreferencesArray(newUserPreferences), undefined, false);
  }
};

const SearchPageLayoutState = ({ children }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  const userPreferences = useContext(UserPreferencesContext);
  const viewType = useContext(ViewTypeContext);
  const [state, setState] = useState({
    sidebarIsPinned: _defaultSidebarPinning(currentUser, userPreferences, viewType),
  });

  const _onSidebarPinningChange = (newIsPinned) => _updateUserSidebarPinning(currentUser, userPreferences, viewType, newIsPinned);

  const getLayoutState = (stateKey: string) => {
    return state[stateKey];
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
