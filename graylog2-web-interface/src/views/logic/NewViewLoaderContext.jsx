// @flow strict
import * as React from 'react';
import View from './views/View';

export type NewViewLoaderContextType = () => Promise<?View>;

const NewViewLoaderContext = React.createContext<NewViewLoaderContextType>(() => Promise.resolve());

export default NewViewLoaderContext;
