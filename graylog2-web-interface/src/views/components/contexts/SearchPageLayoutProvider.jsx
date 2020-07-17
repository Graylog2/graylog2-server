// @flow strict
import * as React from 'react';
import { useContext, useState } from 'react';
import { merge, isEmpty } from 'lodash';
import PropTypes from 'prop-types';

import type { User } from 'stores/users/UsersStore';
import View, { type ViewType } from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import UserPreferencesContext, { type UserPreferences } from 'contexts/UserPreferencesContext';
import CurrentUserContext from 'contexts/CurrentUserContext';
import CombinedProvider from 'injection/CombinedProvider';
import Store from 'logic/local-storage/Store';

import SearchPageLayoutContext from './SearchPageLayoutContext';

const { PreferencesActions } = CombinedProvider.get('Preferences');

type Props = {
  children: React.Node,
  currentUser: ?User,
  userPreferences: UserPreferences,
  viewType: ?ViewType,
};

export const defaultLayoutConfig = (currentUser: ?User, userPreferences?: UserPreferences) => {
  let dashboardSidebarIsPinned = false;
  let searchSidebarIsPinned = false;

  if (currentUser?.id === 'local:admin') {
    searchSidebarIsPinned = Store.get('searchSidebarIsPinned');
    dashboardSidebarIsPinned = Store.get('dashboardSidebarIsPinned');
  } else if (!isEmpty(userPreferences)) {
    searchSidebarIsPinned = userPreferences.searchSidebarIsPinned;
    dashboardSidebarIsPinned = userPreferences.dashboardSidebarIsPinned;
  }

  return {
    sidebar: {
      searchSidebarIsPinned,
      dashboardSidebarIsPinned,
    },
  };
};

const createUserPreferencesArray = (userPreferences) => {
  return Object.entries(userPreferences).map(([name, value]) => ({
    name,
    value,
  }));
};

const sidebarPinningPreferenceKey = (viewType: ?ViewType): string => {
  switch (viewType) {
    case View.Type.Dashboard:
      return 'dashboardSidebarIsPinned';
    default:
      return 'searchSidebarIsPinned';
  }
};

const toggleSidebarPinning = (config, setConfig, currentUser, userPreferences, viewType) => {
  const preferenceKey = sidebarPinningPreferenceKey(viewType);
  const newState = !config.sidebar[preferenceKey];
  const newLayoutConfig = {
    ...config,
    sidebar: {
      ...config.sidebar,
      [preferenceKey]: newState,
    },
  };

  setConfig(newLayoutConfig);

  // eslint-disable-next-line camelcase
  if (currentUser?.read_only) {
    Store.set(preferenceKey, newState);
  } else {
    const newUserPreferences = {
      ...userPreferences,
      [preferenceKey]: newState,
    };
    PreferencesActions.saveUserPreferences(currentUser?.username, createUserPreferencesArray(newUserPreferences), undefined, false);
  }
};

const SearchPageLayoutProvider = ({ children, currentUser, userPreferences, viewType }: Props) => {
  const initialLayoutConfig = defaultLayoutConfig(currentUser, userPreferences);
  const [config, setConfig] = useState(initialLayoutConfig);
  const actions = { toggleSidebarPinning: () => toggleSidebarPinning(config, setConfig, currentUser, userPreferences, viewType) };
  const configHelpers = { sidebar: { isPinned: () => config.sidebar[sidebarPinningPreferenceKey(viewType)] } };
  const configWithHelpers = merge(config, configHelpers);

  return (
    <SearchPageLayoutContext.Provider value={{ config: configWithHelpers, actions }}>
      {children}
    </SearchPageLayoutContext.Provider>
  );
};

SearchPageLayoutProvider.propTypes = {
  children: PropTypes.node.isRequired,
  userPreferences: PropTypes.object,
};

SearchPageLayoutProvider.defaultProps = {
  userPreferences: {},
};

const SearchPageLayoutProviderWithContext = ({ ...rest }: { children: React.Node }) => {
  const currentUser = useContext(CurrentUserContext);
  const userPreferences = useContext(UserPreferencesContext);
  const viewType = useContext(ViewTypeContext);

  return (
    <SearchPageLayoutProvider {...rest} userPreferences={userPreferences} currentUser={currentUser} viewType={viewType} />
  );
};

export default SearchPageLayoutProviderWithContext;
