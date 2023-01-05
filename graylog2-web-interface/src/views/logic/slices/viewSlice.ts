import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';

import type View from 'views/logic/views/View';
import type { ViewState } from 'views/types';

export const viewSlice = createSlice({
  name: 'view',
  initialState: {
    view: undefined,
    isDirty: false,
    isNew: false,
  },
  reducers: {
    load: (state: ViewState, action: PayloadAction<View>) => {
      return {
        view: action.payload,
        isDirty: false,
        isNew: true,
      };
    },
  },
});
export const { load } = viewSlice.actions;
export default viewSlice.reducer;
