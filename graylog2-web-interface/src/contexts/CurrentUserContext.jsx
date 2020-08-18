// @flow strict
import * as React from 'react';

import type { UserJSON } from 'logic/users/User';

import { singleton } from '../views/logic/singleton';

const CurrentUserContext = React.createContext<?UserJSON>();

export default singleton('contexts.CurrentUserContext', () => CurrentUserContext);
