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
import { singletonStore, singletonActions } from 'logic/singleton';

export const SingleNodeActions = singletonActions(
  'core.SingleNode',
  () => Reflux.createActions({
    get: { asyncResult: true },
  }),
);

export const SingleNodeStore = singletonStore(
  'core.SingleNode',
  () => Reflux.createStore({
    listenables: [SingleNodeActions],
    sourceUrl: '/system/cluster/node',
    node: undefined,

    init() {
      this._propagateState();
    },

    getInitialState() {
      return this._getNodeInfo();
    },

    _getNodeInfo() {
      return { node: this.node };
    },

    _propagateState() {
      this.trigger(this._getNodeInfo());
    },

    get(nodeId) {
      const promise = fetch('GET', URLUtils.qualifyUrl(nodeId ? URLUtils.concatURLPath(this.sourceUrl, nodeId) : this.sourceUrl))
        .then((response) => {
          this.node = response;
          this._propagateState();
        });

      SingleNodeActions.get.promise(promise);
    },
  }),
);
