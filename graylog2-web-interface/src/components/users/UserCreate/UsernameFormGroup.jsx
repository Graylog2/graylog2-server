// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';

import { FormikFormGroup } from 'components/common';
import User from 'logic/users/User';

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
