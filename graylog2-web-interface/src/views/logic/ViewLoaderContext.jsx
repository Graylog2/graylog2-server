// @flow strict
import * as React from 'react';

export type ViewLoaderContextType = string => mixed;

const ViewLoaderContext = React.createContext<?ViewLoaderContextType>();

export default ViewLoaderContext;
