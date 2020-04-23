// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { get } from 'lodash';

import { useStore } from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import CurrentUserContext from './CurrentUserContext';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');
const CurrentUserProvider = ({ children }: { children: React.Node }) => {
  const currentUser = useStore(CurrentUserStore, (state) => get(state, 'currentUser'));
  return currentUser
    ? (
      <CurrentUserContext.Provider value={currentUser}>
        {children}
      </CurrentUserContext.Provider>
    )
    : children;
};

CurrentUserProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default CurrentUserProvider;
