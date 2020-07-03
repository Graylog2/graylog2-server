// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { useState } from 'react';

import type { User } from 'stores/users/UsersStore';
import UserPreferencesContext, { type UserPreferences } from 'contexts/UserPreferencesContext';
import PreferencesStore from 'stores/users/PreferencesStore';
import CurrentUserContext from 'contexts/CurrentUserContext';

import SearchPageLayoutContext from './SearchPageLayoutContext';

type Props = {
  children: React.Node,
  currentUser: ?User,
  userPreferences: UserPreferences,
};

export const defaultLayoutConfig = (userPreferences?: UserPreferences) => ({
  sidebar: {
    isPinned: userPreferences?.searchSidebarIsPinned ?? false,
  },
});

const createUserPreferencesArray = (userPreferences) => {
  return Object.entries(userPreferences).map(([name, value]) => ({
    name,
    value,
  }));
};

const toggleSidebarPinning = (config, setConfig, userName, userPreferences) => {
  const newState = !config.sidebar.isPinned;
  const newLayoutConfig = {
    ...config,
    sidebar: {
      ...config.sidebar,
      isPinned: newState,
    },
  };
  const newUserPreferences = {
    ...userPreferences,
    searchSidebarIsPinned: newState,
  };

  setConfig(newLayoutConfig);

  PreferencesStore.saveUserPreferences(userName, createUserPreferencesArray(newUserPreferences), undefined, false);
};

const SearchPageLayoutProvider = ({ children, currentUser, userPreferences }: Props) => {
  const [config, setConfig] = useState(defaultLayoutConfig(userPreferences));
  const actions = { toggleSidebarPinning: () => toggleSidebarPinning(config, setConfig, currentUser?.username, userPreferences) };

  return (
    <SearchPageLayoutContext.Provider value={{ config, actions }}>
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
