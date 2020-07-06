// @flow strict
import * as React from 'react';
import { useContext, useState } from 'react';
import { merge } from 'lodash';
import PropTypes from 'prop-types';

import type { User } from 'stores/users/UsersStore';
import View, { type ViewType } from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import UserPreferencesContext, { type UserPreferences } from 'contexts/UserPreferencesContext';
import CurrentUserContext from 'contexts/CurrentUserContext';
import CombinedProvider from 'injection/CombinedProvider';

import SearchPageLayoutContext from './SearchPageLayoutContext';

const { PreferencesActions } = CombinedProvider.get('Preferences');

type Props = {
  children: React.Node,
  currentUser: ?User,
  userPreferences: UserPreferences,
};

export const defaultLayoutConfig = (userPreferences?: UserPreferences) => ({
  sidebar: {
    searchSidebarIsPinned: userPreferences?.searchSidebarIsPinned ?? false,
    dashboardSidebarIsPinned: userPreferences?.dashboardSidebarIsPinned ?? false,
  },
});

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

const toggleSidebarPinning = (config, setConfig, userName, userPreferences, viewType) => {
  const preferenceKey = sidebarPinningPreferenceKey(viewType);
  const newState = !config.sidebar[preferenceKey];
  const newLayoutConfig = {
    ...config,
    sidebar: {
      ...config.sidebar,
      [preferenceKey]: newState,
    },
  };
  const newUserPreferences = {
    ...userPreferences,
    [preferenceKey]: newState,
  };

  setConfig(newLayoutConfig);

  PreferencesActions.saveUserPreferences(userName, createUserPreferencesArray(newUserPreferences), undefined, false);
};

const SearchPageLayoutProvider = ({ children, currentUser, userPreferences }: Props) => {
  const viewType = useContext(ViewTypeContext);
  const [config, setConfig] = useState(defaultLayoutConfig(userPreferences));
  const actions = { toggleSidebarPinning: () => toggleSidebarPinning(config, setConfig, currentUser?.username, userPreferences, viewType) };
  const configHelpers = { sidebar: { isPinned: () => config.sidebar[sidebarPinningPreferenceKey(viewType)] } };
  const conigWithHelpers = merge(config, configHelpers);

  return (
    <SearchPageLayoutContext.Provider value={{ config: conigWithHelpers, actions }}>
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

const SearchPageLayoutProviderWithContext = ({ ...rest }: { children: React.Node }) => (
  <CurrentUserContext.Consumer>
    {(currentUser) => (
      <UserPreferencesContext.Consumer>
        {(userPreferences) => (
          <SearchPageLayoutProvider {...rest} userPreferences={userPreferences} currentUser={currentUser} />
        )}
      </UserPreferencesContext.Consumer>
    )}
  </CurrentUserContext.Consumer>
);

export default SearchPageLayoutProviderWithContext;
