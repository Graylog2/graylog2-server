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

const InputStaticFieldsStore = Reflux.createStore({
  listenables: [],
  sourceUrl: (inputId) => `/system/inputs/${inputId}/staticfields`,

  create(input, name, value) {
    const url = URLUtils.qualifyUrl(this.sourceUrl(input.id));
    const promise = fetch('POST', url, { key: name, value: value });

    promise
      .then(
        (response) => {
          this.trigger({});
          UserNotification.success(`Static field '${name}' added to '${input.title}' successfully`);

          return response;
        },
        (error) => {
          UserNotification.error(`Adding static field to input failed with: ${error}`,
            `Could not add static field to input '${input.title}'`);
        },
      );

    return promise;
  },

  destroy(input, name) {
    const url = URLUtils.qualifyUrl(`${this.sourceUrl(input.id)}/${name}`);
    const promise = fetch('DELETE', url);

    promise
      .then(
        (response) => {
          this.trigger({});
          UserNotification.success(`Static field '${name}' removed from '${input.title}' successfully`);

          return response;
        },
        (error) => {
          UserNotification.error(`Removing static field from input failed with: ${error}`,
            `Could not remove static field '${name} from input '${input.title}'`);
        },
      );

    return promise;
  },
});

export default InputStaticFieldsStore;
