// @flow strict
import * as React from 'react';

import { singleton } from '../../logic/singleton';

export type TRenderCompletionCallback = () => void;

const RenderCompletionCallback = React.createContext<TRenderCompletionCallback>(() => {});

export default singleton('views.components.widgets.RenderCompletionCallback', () => RenderCompletionCallback);
