// @flow strict
import * as React from 'react';
import { singleton } from 'views/logic/singleton';
import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';

const HighlightingRules = React.createContext<?Array<HighlightingRule>>();

export default singleton('contexts.HighlightingRules', () => HighlightingRules);
