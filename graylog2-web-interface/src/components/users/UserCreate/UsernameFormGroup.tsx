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
import * as Immutable from 'immutable';

import User from 'logic/users/User';
import { FormikFormGroup } from 'components/common';

type Props = {
  users?: Immutable.List<User>,
};

const UsernameFormGroup = ({ users }: Props) => {
  const _validate = (value) => {
    let error;
    const usernameExists = users && !!users.some((user) => user.username === value);

    if (usernameExists) {
      error = 'Username is already taken';
    }

    return error;
  };

  return (
    <FormikFormGroup label="Username"
                     name="username"
                     required
                     validate={_validate}
                     help="Select a unique user name used to log in with." />
  );
};

UsernameFormGroup.defaultProps = {
  users: undefined,
};

export default UsernameFormGroup;
