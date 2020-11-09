// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import PropTypes from 'prop-types';

import UserPreferencesContext from './UserPreferencesContext';
import CurrentUserContext from './CurrentUserContext';

const CurrentUserPreferencesProvider = ({ children }: { children: React.Node }) => {
  const currentUser = useContext(CurrentUserContext);
  const preferences = currentUser?.preferences;

  return preferences
    ? (
      <UserPreferencesContext.Provider value={preferences}>
        {children}
      </UserPreferencesContext.Provider>
    )
    : children;
};

CurrentUserPreferencesProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default CurrentUserPreferencesProvider;
