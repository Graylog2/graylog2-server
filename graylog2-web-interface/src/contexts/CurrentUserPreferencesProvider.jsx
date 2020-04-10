// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { get } from 'lodash';

import { useStore } from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import UserPreferencesContext from './UserPreferencesContext';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const CurrentUserPreferencesProvider = ({ children }: { children: React.Node }) => {
  const preferences = useStore(CurrentUserStore, (state) => get(state, 'currentUser.preferences'));
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
