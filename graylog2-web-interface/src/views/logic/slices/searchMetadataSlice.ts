import { createSlice, createSelector } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

import SearchMetadata from 'views/logic/search/SearchMetadata';
import type { RootState } from 'views/types';
import type { AppDispatch } from 'stores/useAppDispatch';
import type Search from 'views/logic/search/Search';
import fetch from 'logic/rest/FetchProvider';
import * as URLUtils from 'util/URLUtils';

const searchMetadataSlice = createSlice({
  name: 'searchMetadata',
  initialState: {
    isLoading: false,
    metadata: undefined,
  },
  reducers: {
    loading: (state) => ({
      ...state,
      isLoading: true,
      metadata: undefined,
    }),
    finishedLoading: (state, action: PayloadAction<SearchMetadata>) => ({
      ...state,
      isLoading: false,
      metadata: action.payload,
    }),
  },
});

const { finishedLoading, loading } = searchMetadataSlice.actions;

export const searchMetadataSliceReducer = searchMetadataSlice.reducer;

export const selectSearchMetadataState = (state: RootState) => state.searchMetadata;
export const selectSearchMetadata = createSelector(selectSearchMetadataState, (state) => state.metadata);
export const selectHasUndeclaredParameters = createSelector(selectSearchMetadata, (searchMetadata: SearchMetadata) => searchMetadata.undeclared.size > 0);

const parseSearchUrl = URLUtils.qualifyUrl('/views/search/metadata');

export const parseSearch = (search: Search) => async (dispatch: AppDispatch) => {
  dispatch(loading());

  return fetch('POST', parseSearchUrl, JSON.stringify(search))
    .then((response) => SearchMetadata.fromJSON(response), () => undefined)
    .then((result) => dispatch(finishedLoading(result)));
};
