// @flow strict
import * as React from 'react';
import { ActionContext } from '../ActionContext';

const ActionContextProvider = React.createContext<ActionContext>(ActionContext.empty());

export default ActionContextProvider;
