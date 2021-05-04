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
import Reflux from 'reflux';

import * as URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import { singletonStore } from 'views/logic/singleton';
import { Store } from 'stores/StoreTypes';

const functionsUrl = URLUtils.qualifyUrl('/views/functions');

type AggregationFunction = { type: string, description: string };

type AggregationFunctionsStoreState = {
  [functionName: string]: AggregationFunction | undefined,
}

const AggregationFunctionsStore: Store<AggregationFunctionsStoreState> = singletonStore(
  'views.AggregationFunctions',
  () => Reflux.createStore({
    init() {
      this.refresh();
    },

    getInitialState() {
      return this._state();
    },

    refresh() {
      fetch('GET', functionsUrl).then((response) => {
        this.functions = response;
        this._trigger();
      });
    },
    _state() {
      return this.functions;
    },
    _trigger() {
      this.trigger(this._state());
    },
  }),
);

export default AggregationFunctionsStore;
