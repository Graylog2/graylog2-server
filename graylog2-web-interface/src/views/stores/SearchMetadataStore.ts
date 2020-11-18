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
// @flow strict
import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import SearchMetadata from 'views/logic/search/SearchMetadata';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import type { RefluxActions } from 'stores/StoreTypes';

const parseSearchUrl = URLUtils.qualifyUrl('/views/search/metadata');
const parseSearchIdUrl = (id) => URLUtils.qualifyUrl(`/views/search/metadata/${id}`);

export type SearchMetadataActionsType = RefluxActions<{
  parseSearch: (any) => Promise<SearchMetadata>,
  parseSearchId: (string) => Promise<SearchMetadata>,
}>;

export const SearchMetadataActions: SearchMetadataActionsType = singletonActions(
  'views.SearchMetadata',
  () => Reflux.createActions({
    parseSearch: { asyncResult: true },
    parseSearchId: { asyncResult: true },
  }),
);

export const SearchMetadataStore = singletonStore(
  'views.SearchMetadata',
  () => Reflux.createStore({
    listenables: [SearchMetadataActions],

    state: SearchMetadata.empty(),

    getInitialState() {
      return this.state;
    },

    parseSearch(searchRequest): Promise<SearchMetadata> {
      const promise = fetch('POST', parseSearchUrl, JSON.stringify(searchRequest))
        .then(SearchMetadata.fromJSON)
        .then((metadata) => {
          this.state = metadata;
          this._trigger();

          return this.state;
        });

      SearchMetadataActions.parseSearch.promise(promise);

      return promise;
    },

    parseSearchId(searchId: string): Promise<SearchMetadata> {
      const promise = fetch('GET', parseSearchIdUrl, searchId)
        .then(SearchMetadata.fromJSON)
        .then((metadata) => {
          this.state = metadata;
          this._trigger();

          return this.state;
        });

      SearchMetadataActions.parseSearchId.promise(promise);

      return promise;
    },

    _trigger() {
      this.trigger(this.state);
    },
  }),
);
