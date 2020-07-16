// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';

import User from 'logic/users/User';

import FormField from '../form/FormField';

type Props = {
  users?: Immutable.List<User>,
};

const FormikUsername = ({ users }: Props) => {
  const _validate = (value) => {
    let error;
    const usernameExists = users && !!users.some((user) => user.username === value);

    if (usernameExists) {
      error = 'Username is already taken';
    }

    return error;
  };

  return (
    <FormField label="Username"
               name="username"
               required
               validate={_validate}
               help="Select a unique user name used to log in with." />
  );
};

FormikUsername.defaultProps = {
  users: undefined,
};

export default FormikUsername;
