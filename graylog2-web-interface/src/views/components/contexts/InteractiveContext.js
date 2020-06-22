// @flow strict
import * as React from 'react';

import { singleton } from '../../logic/singleton';

const InteractiveContext = React.createContext<boolean>(true);

export default singleton('views.components.contexts.InteractiveContext', () => InteractiveContext);
