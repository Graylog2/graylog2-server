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

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';

const DeflectorActions = ActionsProvider.getActions('Deflector');

const DeflectorStore = Reflux.createStore({
  listenables: [DeflectorActions],
  deflector: {
    info: undefined,
  },
  getInitialState() {
    return { deflector: this.deflector };
  },
  cycle(indexSetId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.DeflectorApiController.cycle(indexSetId).url);
    const promise = fetch('POST', url);

    DeflectorActions.cycle.promise(promise);
  },
  list(indexSetId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.DeflectorApiController.list(indexSetId).url);
    const promise = fetch('GET', url).then((info) => {
      this.deflector.info = info;
      this.trigger({ deflector: this.deflector });
    });

    DeflectorActions.list.promise(promise);
  },
});

export default DeflectorStore;
