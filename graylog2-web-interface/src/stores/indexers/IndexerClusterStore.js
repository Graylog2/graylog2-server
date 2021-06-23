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
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';

const IndexerClusterActions = ActionsProvider.getActions('IndexerCluster');

const IndexerClusterStore = Reflux.createStore({
  listenables: [IndexerClusterActions],
  state: {},
  init() {
    this.update();
  },
  update() {
    Promise.all([
      this.health().then((health) => {
        this.state.health = health;
      }),
      this.name().then((response) => {
        this.state.name = response.name;
      }),
    ]).then(() => this.trigger(this.state));
  },
  getInitialState() {
    return this.state;
  },
  health() {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexerClusterApiController.health().url);
    const promise = fetch('GET', url);

    IndexerClusterActions.health.promise(promise);

    return promise;
  },
  name() {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexerClusterApiController.name().url);
    const promise = fetch('GET', url);

    IndexerClusterActions.name.promise(promise);

    return promise;
  },
});

export default IndexerClusterStore;
