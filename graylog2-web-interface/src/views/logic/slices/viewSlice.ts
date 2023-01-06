import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppDispatch } from 'src/stores/useAppDispatch';

import type View from 'views/logic/views/View';
import type { ViewState } from 'views/types';
import type { QueryId } from 'views/logic/queries/Query';
import type ViewStateType from 'views/logic/views/ViewState';
import NewQueryActionHandler from 'views/logic/NewQueryActionHandler';
import type Query from 'views/logic/queries/Query';

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
    addQuery: (state: ViewState, action: PayloadAction<[Query, ViewStateType]>) => {
      const [query, viewState] = action.payload;
      const { view } = state ?? {};
      const { search } = view;
      const newQueries = search.queries.add(query);
      const newViewStates = view.state.set(query.id, viewState);
      const newView = view.toBuilder()
        .search(search.toBuilder()
          .queries(newQueries)
          .build())
        .state(newViewStates)
        .build();

      return {
        ...state,
        view: newView,
        activeQuery: query.id,
      };
    },
  },
});

export const createQuery = () => async (dispatch: AppDispatch) => {
  const [query, state] = await NewQueryActionHandler();
  dispatch(viewSlice.actions.addQuery([query, state]));
};

export const { load, selectQuery } = viewSlice.actions;
export default viewSlice.reducer;
