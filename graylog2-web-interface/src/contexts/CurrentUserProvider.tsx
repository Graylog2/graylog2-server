/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import PropTypes from 'prop-types';
import get from 'lodash/get';

import { useStore } from 'stores/connect';
import User from 'logic/users/User';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';

import CurrentUserContext from './CurrentUserContext';

const CurrentUserProvider = ({ children }) => {
  const currentUserJSON = useStore(CurrentUserStore, (state) => get(state, 'currentUser'));
  const currentUser = currentUserJSON ? User.fromJSON(currentUserJSON) : undefined;

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
