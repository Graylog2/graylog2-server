import * as React from 'react';
import { useContext } from 'react';

export type RuleBuilderContextType = {
  useHighlightedOutput: [
    string,
    React.Dispatch<React.SetStateAction<string>>
  ]
}

const RuleBuilderContext = React.createContext<RuleBuilderContextType | null>(null);

const useRuleBuilder = (): RuleBuilderContextType => {
  const context = useContext(RuleBuilderContext);

  if (!context) {
    throw new Error('useRuleBuilder must be used within a RuleBuilderProvider');
  }

  return context;
};

export { useRuleBuilder };
export default RuleBuilderContext;
