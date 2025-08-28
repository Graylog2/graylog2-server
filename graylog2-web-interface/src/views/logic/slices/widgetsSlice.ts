import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

import type { ViewsDispatch } from 'views/stores/useViewsDispatch';

export type WidgetsState = {
  newWidget: string | undefined;
};
const widgetsSlice = createSlice({
  name: 'widgets',
  initialState: {
    newWidget: undefined,
  },
  reducers: {
    setNewWidget: (state, action: PayloadAction<string>) => ({
      ...state,
      newWidget: action.payload,
    }),
  },
});
export const widgetsSliceReducer = widgetsSlice.reducer;
export const setNewWidget = (id: string) => (dispatch: ViewsDispatch) => {
  const result = dispatch(widgetsSlice.actions.setNewWidget(id));
  setTimeout(() => dispatch(widgetsSlice.actions.setNewWidget(undefined)), 1000);

  return result;
};

export const selectNewWidget = (state: { widgets: WidgetsState }) => state.widgets.newWidget;
