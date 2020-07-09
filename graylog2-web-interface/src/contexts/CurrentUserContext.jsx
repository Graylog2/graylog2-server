// @flow strict
import * as React from 'react';

import type { UserJSON as User } from 'stores/users/UsersStore.js';

import { singleton } from '../views/logic/singleton';

const CurrentUserContext = React.createContext<?User>();

export default singleton('contexts.CurrentUserContext', () => CurrentUserContext);
