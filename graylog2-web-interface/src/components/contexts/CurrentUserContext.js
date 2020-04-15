// @flow strict
import * as React from 'react';
import type { User } from 'stores/users/UsersStore';

const CurrentUserContext = React.createContext<?User>();

export default CurrentUserContext;
