import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import type { AppDispatch } from 'stores/useAppDispatch';
import type { GetState, WidgetPositions } from 'views/types';
import { selectWidgetPositions } from 'views/logic/slices/widgetSelectors';
import { selectActiveQuery, selectActiveViewState } from 'views/logic/slices/viewSelectors';
import { updateViewState } from 'views/logic/slices/viewSlice';

export const updateWidgetPositions = (newWidgetPositions: WidgetPositions) => (dispatch: AppDispatch, getState: GetState) => {
  const activeQuery = selectActiveQuery(getState());
  const activeViewState = selectActiveViewState(getState());
  const newViewState = activeViewState.toBuilder()
    .widgetPositions(newWidgetPositions)
    .build();

  return dispatch(updateViewState(activeQuery, newViewState));
};

export const updateWidgetPosition = (id: string, newWidgetPosition: WidgetPosition) => (dispatch: AppDispatch, getState: GetState) => {
  const widgetPositions = selectWidgetPositions(getState());
  const newWidgetPositions = { ...widgetPositions, [id]: newWidgetPosition };

  return dispatch(updateWidgetPositions(newWidgetPositions));
};
