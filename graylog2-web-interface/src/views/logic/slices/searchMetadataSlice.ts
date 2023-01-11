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
import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

import SearchMetadata from 'views/logic/search/SearchMetadata';
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

const parseSearchUrl = URLUtils.qualifyUrl('/views/search/metadata');

export const parseSearch = (search: Search) => async (dispatch: AppDispatch) => {
  dispatch(loading());

  return fetch('POST', parseSearchUrl, JSON.stringify(search))
    .then((response) => SearchMetadata.fromJSON(response), () => undefined)
    .then((result) => dispatch(finishedLoading(result)));
};
