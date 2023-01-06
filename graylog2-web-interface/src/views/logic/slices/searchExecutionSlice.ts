import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import type { SearchExecution } from 'views/types';

const searchExecutionSlice = createSlice({
  name: 'searchExecution',
  initialState: {
    widgetsToSearch: undefined,
    executionState: SearchExecutionState.empty(),
    globalOverride: GlobalOverride.empty(),
    isLoading: false,
    result: undefined,
  } as SearchExecution,
  reducers: {
    loading: (state) => ({
      ...state,
      isLoading: true,
    }),
    finishedLoading: (state, action: PayloadAction<SearchExecution['result']>) => ({
      ...state,
      isLoading: false,
      result: action.payload,
    }),
  },
});

export const searchExecutionSliceReducer = searchExecutionSlice.reducer;
