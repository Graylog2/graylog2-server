/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import { createSelector } from '@reduxjs/toolkit';

import type { RootState } from 'views/types';

export const selectSearchExecutionRoot = (state: RootState) => state.searchExecution;

export const selectSearchExecutionState = createSelector(selectSearchExecutionRoot, (state) => state.executionState);
export const selectJobIds = createSelector(selectSearchExecutionRoot, (state) => state.jobIds);
export const selectWidgetsToSearch = createSelector(selectSearchExecutionRoot, (state) => state.widgetsToSearch);
export const selectSearchExecutionResult = createSelector(selectSearchExecutionRoot, (state) => state.result);
export const selectSearchJobId = createSelector(selectSearchExecutionResult, (result) => result?.result?.result?.id);
export const selectGlobalOverride = createSelector(selectSearchExecutionState, (executionState) => executionState.globalOverride);
export const selectParameterBindings = createSelector(selectSearchExecutionState, (executionState) => executionState.parameterBindings);
