import { createSelector } from '@reduxjs/toolkit';

import { selectActiveViewState } from 'views/logic/slices/viewSelectors';

export const selectHighlightingRules = createSelector(selectActiveViewState, (viewState) => viewState?.formatting?.highlighting ?? []);
