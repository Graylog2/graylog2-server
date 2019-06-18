// @flow strict
import { DEFAULT_CUSTOM_HIGHLIGHT_RANGE } from 'views/Constants';
import { HighlightingRulesActions, HighlightingRulesStore } from 'views/stores/HighlightingRulesStore';
import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import type { ValueActionHandler, ValueActionHandlerCondition, ValueActionHandlerConditionProps } from './ValueActionHandler';

const randomColor = () => DEFAULT_CUSTOM_HIGHLIGHT_RANGE[
  Math.floor(Math.random() * DEFAULT_CUSTOM_HIGHLIGHT_RANGE.length)
];

const HighlightValueHandler: ValueActionHandler = (queryId: string, field: string, value: any) => {
  return HighlightingRulesActions.add(
    HighlightingRule.builder()
      .field(field)
      .value(value)
      .color(randomColor())
      .build(),
  );
};

const condition: ValueActionHandlerCondition = ({ field, value }: ValueActionHandlerConditionProps) => {
  const highlightingRules = HighlightingRulesStore.getInitialState();
  return highlightingRules.find(({ field: f, value: v }) => (field === f && value === v)) === undefined;
};

HighlightValueHandler.condition = condition;

export default HighlightValueHandler;
