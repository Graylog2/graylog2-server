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

import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import * as URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import CombinedProvider from 'injection/CombinedProvider';

const { CodecTypesActions } = CombinedProvider.get('CodecTypes');

const CodecTypesStore = Reflux.createStore({
  listenables: [CodecTypesActions],
  codecTypes: undefined,

  getInitialState() {
    return { codecTypes: this.codecTypes };
  },

  list() {
    const promise = fetch('GET', URLUtils.qualifyUrl(ApiRoutes.CodecTypesController.list().url));

    promise.then(
      (response) => {
        this.codecTypes = response;
        this.trigger(this.getInitialState());
      },
      (error) => {
        UserNotification.error(`Fetching codec types failed with status: ${error}`,
          'Could not retrieve codec types');
      },
    );

    CodecTypesActions.list.promise(promise);
  },
});

export default CodecTypesStore;
