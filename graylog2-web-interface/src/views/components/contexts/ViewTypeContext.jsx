// @flow strict
import * as React from 'react';

import type { ViewType } from 'views/logic/views/View';

const ViewTypeContext = React.createContext<?ViewType>();

export default ViewTypeContext;
