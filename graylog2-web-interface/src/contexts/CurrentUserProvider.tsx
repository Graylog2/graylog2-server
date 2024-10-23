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
import get from 'lodash/get';

import { useStore } from 'stores/connect';
import User from 'logic/users/User';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import Spinner from 'components/common/Spinner';

import CurrentUserContext from './CurrentUserContext';

type CurrentUserProviderProps = {
  children: React.ReactNode;
};

const CurrentUserProvider = ({
  children,
}: CurrentUserProviderProps) => {
  const currentUserJSON = useStore(CurrentUserStore, (state) => get(state, 'currentUser'));
  const currentUser = currentUserJSON ? User.fromJSON(currentUserJSON) : undefined;

  if (!currentUser) {
    return <Spinner />;
  }

  return (
    <CurrentUserContext.Provider value={currentUser}>
      {children}
    </CurrentUserContext.Provider>
  );
};

export default CurrentUserProvider;
