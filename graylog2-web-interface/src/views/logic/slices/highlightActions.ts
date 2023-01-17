import HighlightingRule, { randomColor } from 'views/logic/views/formatting/highlighting/HighlightingRule';
import type { AppDispatch } from 'stores/useAppDispatch';
import type { GetState } from 'views/types';
import { selectActiveViewState, selectActiveQuery } from 'views/logic/slices/viewSelectors';
import FormattingSettings from 'views/logic/views/formatting/FormattingSettings';
import { updateViewState } from 'views/logic/slices/viewSlice';
import { selectHighlightingRules } from 'views/logic/slices/highlightSelectors';

export const addHighlightingRule = (rule: HighlightingRule) => async (dispatch: AppDispatch, getState: GetState) => {
  const activeQuery = selectActiveQuery(getState());
  const activeViewState = selectActiveViewState(getState());
  const { formatting = FormattingSettings.empty() } = activeViewState;
  const newFormatting = formatting.toBuilder()
    .highlighting([...formatting.highlighting, rule])
    .build();

  const newViewState = activeViewState
    .toBuilder()
    .formatting(newFormatting)
    .build();

  return dispatch(updateViewState(activeQuery, newViewState));
};

export const createHighlightingRule = (field: string, value: any) => async (dispatch: AppDispatch) => {
  const newRule = HighlightingRule.builder()
    .field(field)
    .value(value)
    .color(randomColor())
    .build();

  return dispatch(addHighlightingRule(newRule));
};

export const updateHighlightingRules = (rules: Array<HighlightingRule>) => async (dispatch: AppDispatch, getState: GetState) => {
  const activeQuery = selectActiveQuery(getState());
  const currentViewState = selectActiveViewState(getState());
  const { formatting = FormattingSettings.empty() } = currentViewState;
  const newViewState = currentViewState.toBuilder()
    .formatting(formatting.toBuilder().highlighting(rules).build())
    .build();

  return dispatch(updateViewState(activeQuery, newViewState));
};

export const updateHighlightingRule = (rule: HighlightingRule, payload: Partial<HighlightingRule>) => async (dispatch: AppDispatch, getState: GetState) => {
  const highlightingRules = selectHighlightingRules(getState());

  if (Object.entries(payload).length === 0) {
    return Promise.resolve();
  }

  const newRuleBuilder = Object.entries(payload)
    .reduce((builder, [key, value]) => builder[key](value), rule.toBuilder());
  const newRule = newRuleBuilder.build();

  const newHighlightingRules = highlightingRules.map((currentRule) => (currentRule === rule ? newRule : currentRule));

  return dispatch(updateHighlightingRules(newHighlightingRules));
};

export const removeHighlightingRule = (rule: HighlightingRule) => async (dispatch: AppDispatch, getState: GetState) => {
  const highlightingRules = selectHighlightingRules(getState());

  const newHighlightingRules = highlightingRules.filter((r) => r !== rule);

  return dispatch(updateHighlightingRules(newHighlightingRules));
};
