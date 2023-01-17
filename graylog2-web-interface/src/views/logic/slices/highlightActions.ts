import HighlightingRule, { randomColor } from 'views/logic/views/formatting/highlighting/HighlightingRule';
import type { AppDispatch } from 'stores/useAppDispatch';
import type { GetState } from 'views/types';
import { selectActiveViewState, selectActiveQuery } from 'views/logic/slices/viewSelectors';
import FormattingSettings from 'views/logic/views/formatting/FormattingSettings';
import { updateViewState } from 'views/logic/slices/viewSlice';

export const addHighlightingRule = (rule: HighlightingRule) => (dispatch: AppDispatch, getState: GetState) => {
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

export const createHighlightingRule = (field: string, value: any) => (dispatch: AppDispatch) => {
  const newRule = HighlightingRule.builder()
    .field(field)
    .value(value)
    .color(randomColor())
    .build();

  return dispatch(addHighlightingRule(newRule));
};
