// @flow strict
import * as React from 'react';
import type { User } from 'stores/users/UsersStore';
import { singleton } from '../views/logic/singleton';

const CurrentUserContext = React.createContext<?User>();

export default singleton('contexts.CurrentUserContext', () => CurrentUserContext);
