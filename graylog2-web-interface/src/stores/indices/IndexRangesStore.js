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

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';

const IndexRangesActions = ActionsProvider.getActions('IndexRanges');

const IndexRangesStore = Reflux.createStore({
  listenables: [IndexRangesActions],
  indexRanges: undefined,

  getInitialState() {
    return { indexRanges: this.indexRanges };
  },
  init() {
    IndexRangesActions.list();
  },
  list() {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexRangesApiController.list().url);
    const promise = fetch('GET', url).then((response) => {
      this.indexRanges = response.ranges;

      this.trigger(this.getInitialState());
    });

    IndexRangesActions.list.promise(promise);
  },
  recalculate(indexSetId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexRangesApiController.rebuild(indexSetId).url);
    const promise = fetch('POST', url);

    promise
      .then(UserNotification.success('Index ranges will be recalculated shortly'))
      .catch((error) => {
        UserNotification.error(`Could not create a job to start index ranges recalculation, reason: ${error}`,
          'Error starting index ranges recalculation');
      });

    IndexRangesActions.recalculate.promise(promise);
  },
  recalculateIndex(indexName) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexRangesApiController.rebuildSingle(indexName).url);
    const promise = fetch('POST', url);

    promise
      .then(UserNotification.success(`Index ranges for ${indexName} will be recalculated shortly`))
      .catch((error) => {
        UserNotification.error(`Could not create a job to start index ranges recalculation for ${indexName}, reason: ${error}`,
          `Error starting index ranges recalculation for ${indexName}`);
      });

    IndexRangesActions.recalculateIndex.promise(promise);
  },
});

export default IndexRangesStore;
