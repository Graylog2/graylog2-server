import { createSelector } from '@reduxjs/toolkit';

import type { RootState } from 'views/types';

export const selectSearchExecutionRoot = (state: RootState) => state.searchExecution;

export const selectSearchExecutionState = createSelector(selectSearchExecutionRoot, (state) => state.executionState);
export const selectWidgetsToSearch = createSelector(selectSearchExecutionRoot, (state) => state.widgetsToSearch);
export const selectSearchExecutionResult = createSelector(selectSearchExecutionRoot, (state) => state.result);
export const selectGlobalOverride = createSelector(selectSearchExecutionState, (executionState) => executionState.globalOverride);
export const selectParameterBindings = createSelector(selectSearchExecutionState, (executionState) => executionState.parameterBindings);
