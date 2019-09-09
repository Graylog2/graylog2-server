// @flow strict
import * as React from 'react';

import View from 'views/logic/views/View';

export type ViewLoaderContextType = string => Promise<?View>;

const ViewLoaderContext = React.createContext<?ViewLoaderContextType>();

export default ViewLoaderContext;
