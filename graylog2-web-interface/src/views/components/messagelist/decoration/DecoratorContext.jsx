// @flow strict
import * as React from 'react';

import type { ValueRenderer } from './ValueRenderer';

import Highlight from '../Highlight';

type DecoratorList = Array<ValueRenderer>;
const DecoratorContext = React.createContext<DecoratorList>([Highlight]);
export default DecoratorContext;
