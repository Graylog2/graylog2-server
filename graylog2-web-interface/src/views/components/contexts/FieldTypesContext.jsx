// @flow strict
import * as React from 'react';

import { singleton } from 'views/logic/singleton';
import type { FieldTypesStoreState } from 'views/stores/FieldTypesStore';

const FieldTypesContext = React.createContext<?FieldTypesStoreState>();
export default singleton('contexts.FieldTypesContext', () => FieldTypesContext);
