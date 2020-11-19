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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { get } from 'lodash';

import { useStore } from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';

import CurrentUserContext from './CurrentUserContext';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const CurrentUserProvider = ({ children }) => {
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
