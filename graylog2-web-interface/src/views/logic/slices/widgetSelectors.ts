import { createSelector } from '@reduxjs/toolkit';

import { selectActiveViewState } from 'views/logic/slices/viewSelectors';

export const selectWidgetPositions = createSelector(selectActiveViewState, (viewState) => viewState.widgetPositions);
