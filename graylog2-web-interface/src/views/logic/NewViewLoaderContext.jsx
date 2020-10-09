// @flow strict
import * as React from 'react';

export type NewViewLoaderContextType = () => mixed;

const NewViewLoaderContext = React.createContext<NewViewLoaderContextType>(() => {});

export default NewViewLoaderContext;
