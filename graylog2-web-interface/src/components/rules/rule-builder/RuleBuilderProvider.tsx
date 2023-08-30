import * as React from 'react';
import { useMemo, useState } from 'react';

import RuleBuilderContext from './RuleBuilderContext';
import type { RuleBuilderContextType } from './RuleBuilderContext';

type Props = {
  children: React.ReactNode,
};

const RuleBuilderProvider = ({ children }: Props) => {
  const [highlightedOutput, setHighlightedOutput] = useState<string>(undefined);

  const value: RuleBuilderContextType = useMemo(() => ({ useHighlightedOutput: [highlightedOutput, setHighlightedOutput] }), [highlightedOutput]);

  return (
    <RuleBuilderContext.Provider value={value}>
      {children}
    </RuleBuilderContext.Provider>
  );
};

export default RuleBuilderProvider;
