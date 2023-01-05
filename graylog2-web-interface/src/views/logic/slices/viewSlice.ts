import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';

import type View from 'views/logic/views/View';
import type { ViewState } from 'views/types';
import type { QueryId } from 'views/logic/queries/Query';

export const viewSlice = createSlice({
  name: 'view',
  initialState: {
    view: undefined,
    isDirty: false,
    isNew: false,
  },
  reducers: {
    load: (_state: ViewState, action: PayloadAction<View>) => {
      return {
        view: action.payload,
        isDirty: false,
        isNew: true,
      };
    },
    selectQuery: (state: ViewState, action: PayloadAction<QueryId>) => ({
      ...state,
      activeQuery: action.payload,
    }),
  },
});
export const { load, selectQuery } = viewSlice.actions;
export default viewSlice.reducer;
