// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import UserPreferencesContext from './UserPreferencesContext';
import CurrentUserContext from './CurrentUserContext';

const CurrentUserPreferencesProvider = ({ children }: { children: React.Node }) => (
  <CurrentUserContext.Consumer>
    {(currentUser) => {
      const preferences = currentUser?.preferences;
      return preferences
        ? (
          <UserPreferencesContext.Provider value={preferences}>
            {children}
          </UserPreferencesContext.Provider>
        )
        : children;
    }}
  </CurrentUserContext.Consumer>
);

CurrentUserPreferencesProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default CurrentUserPreferencesProvider;
