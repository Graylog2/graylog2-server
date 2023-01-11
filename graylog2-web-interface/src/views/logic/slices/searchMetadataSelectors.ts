import { createSelector } from '@reduxjs/toolkit';

import type { RootState } from 'views/types';
import type SearchMetadata from 'views/logic/search/SearchMetadata';

export const selectSearchMetadataState = (state: RootState) => state.searchMetadata;
export const selectSearchMetadata = createSelector(selectSearchMetadataState, (state) => state.metadata);
export const selectHasUndeclaredParameters = createSelector(selectSearchMetadata, (searchMetadata: SearchMetadata) => searchMetadata?.undeclared?.size > 0);
