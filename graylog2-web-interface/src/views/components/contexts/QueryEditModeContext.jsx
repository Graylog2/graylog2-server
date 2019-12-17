// @flow strict
import * as React from 'react';

import { singleton } from 'views/logic/singleton';

export type QueryEditMode = 'query' | 'widget';

const QueryEditModeContext = React.createContext<QueryEditMode>('query');

export default singleton('views.components.contexts.QueryEditModeContext', () => QueryEditModeContext);
