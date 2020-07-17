// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import PropTypes from 'prop-types';

import View, { type ViewType } from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import UserPreferencesContext from 'contexts/UserPreferencesContext';
import CurrentUserContext from 'contexts/CurrentUserContext';
import CombinedProvider from 'injection/CombinedProvider';
import Store from 'logic/local-storage/Store';

import SearchPageLayoutContext from './SearchPageLayoutContext';
import SearchPageLayoutState from './SearchPageLayoutState';

const { PreferencesActions } = CombinedProvider.get('Preferences');

type Props = {
  children: React.Node,
};

const getPinningPreferenceKey = (viewType: ?ViewType): string => {
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

const getDefaultIsPinned = (currentUser, userPreferences, viewType) => {
  const fallbackDefault = false;
  const sidebarPinningPrefKey = getPinningPreferenceKey(viewType);

  // eslint-disable-next-line camelcase
  if (currentUser?.read_only) {
    return Store.get(sidebarPinningPrefKey) ?? fallbackDefault;
  }

  return userPreferences[sidebarPinningPrefKey] ?? fallbackDefault;
};

const createUserPreferencesArray = (userPreferences) => {
  return Object.entries(userPreferences).map(([name, value]) => ({
    name,
    value,
  }));
};

const _toggleSidebarPinning = (currentUser, userPreferences, viewType, setLayoutState, isPinned) => {
  const sidebarPinningPrefKey = getPinningPreferenceKey(viewType);
  const newPinningState = !isPinned;
  setLayoutState('sidebarIsPinned', newPinningState);

  // eslint-disable-next-line camelcase
  if (currentUser?.read_only) {
    Store.set(sidebarPinningPrefKey, !isPinned);
  } else {
    const newUserPreferences = {
      ...userPreferences,
      [sidebarPinningPrefKey]: newPinningState,
    };
    PreferencesActions.saveUserPreferences(currentUser?.username, createUserPreferencesArray(newUserPreferences), undefined, false);
  }
};

const SearchPageLayoutProvider = ({ children }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  const userPreferences = useContext(UserPreferencesContext);
  const viewType = useContext(ViewTypeContext);
  const defaultIsPinned = getDefaultIsPinned(currentUser, userPreferences, viewType);

  return (
    <SearchPageLayoutState>
      {({ getLayoutState, setLayoutState }) => {
        const config = {
          sidebar: { isPinned: getLayoutState('sidebarIsPinned', defaultIsPinned) },
        };
        const actions = { toggleSidebarPinning: () => _toggleSidebarPinning(currentUser, userPreferences, viewType, setLayoutState, config.sidebar.isPinned) };

        return (
          <SearchPageLayoutContext.Provider value={{ config, actions }}>
            {children}
          </SearchPageLayoutContext.Provider>
        );
      }}

    </SearchPageLayoutState>
  );
};

SearchPageLayoutProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default SearchPageLayoutProvider;
