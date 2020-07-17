// @flow strict
import { useContext, useState } from 'react';

import View, { type ViewType } from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import UserPreferencesContext from 'contexts/UserPreferencesContext';
import CurrentUserContext from 'contexts/CurrentUserContext';
import CombinedProvider from 'injection/CombinedProvider';
import Store from 'logic/local-storage/Store';

const { PreferencesActions } = CombinedProvider.get('Preferences');

const _getPinningPreferenceKey = (viewType: ?ViewType): string => {
  const preferenceKeyMapping = {
    [View.Type.Dashboard]: 'dashboardSidebarIsPinned',
    [View.Type.Search]: 'searchSidebarIsPinned',
  };

  const preferenceKey = preferenceKeyMapping[viewType];

  if (!preferenceKey) {
    throw new Error(`User sidebar pinning preference key is missing for view type ${viewType}`);
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

const SearchPageLayoutState = ({ children }) => {
  const currentUser = useContext(CurrentUserContext);
  const userPreferences = useContext(UserPreferencesContext);
  const viewType = useContext(ViewTypeContext);
  const [state, setState] = useState({
    sidebarIsPinned: _defaultSidebarPinning(currentUser, userPreferences, viewType),
  });

  const _onSidebarPinningChange = (newIsPinned) => _updateUserSidebarPinning(currentUser, userPreferences, viewType, newIsPinned);

  const getLayoutState = (stateKey) => {
    return state[stateKey];
  };

  const setLayoutState = (stateKey, value) => {
    if (stateKey === 'sidebarIsPinned') {
      _onSidebarPinningChange(value);
    }

    setState({ ...state, [stateKey]: value });
  };

  return children({ getLayoutState, setLayoutState });
};

export default SearchPageLayoutState;
