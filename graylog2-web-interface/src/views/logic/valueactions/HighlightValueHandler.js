// @flow strict
import { DEFAULT_CUSTOM_HIGHLIGHT_RANGE } from 'views/Constants';
import { HighlightingRulesActions, HighlightingRulesStore } from 'views/stores/HighlightingRulesStore';
import HighlightingRule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import type { ActionHandlerCondition } from 'views/components/actions/ActionHandler';

import type { ValueActionHandler } from './ValueActionHandler';

const randomColor = () => DEFAULT_CUSTOM_HIGHLIGHT_RANGE[
  Math.floor(Math.random() * DEFAULT_CUSTOM_HIGHLIGHT_RANGE.length)
];

const HighlightValueHandler: ValueActionHandler = ({ field, value }) => {
  if (value === undefined) {
    return Promise.reject(new Error('Unable to add highlighting for missing value.'));
  }
  return HighlightingRulesActions.add(
    HighlightingRule.builder()
      .field(field)
      .value(value)
      .color(randomColor())
      .build(),
  );
};

const isEnabled: ActionHandlerCondition = ({ field, value }) => {
  const highlightingRules = HighlightingRulesStore.getInitialState();
  return highlightingRules.find(({ field: f, value: v }) => (field === f && value === v)) === undefined;
};

HighlightValueHandler.isEnabled = isEnabled;

export default HighlightValueHandler;
