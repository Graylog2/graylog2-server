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

import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';
import { singletonStore } from 'views/logic/singleton';

const CustomizationActions = ActionsProvider.getActions('Customizations');

const urlPrefix = ApiRoutes.ClusterConfigResource.config().url;

type CustomizationsStoreState = {
  customization: {},
};

const CustomizationStore = singletonStore('customization.store', () => Reflux.createStore({
  listenables: [CustomizationActions],

  customization: {},

  getInitialState() {
    return {
      customization: this.customization,
    };
  },

  get(type: string) {
    const promise = fetch('GET', this._url(`/${type}`));

    promise.then((response) => {
      this.customization = { ...this.customization, [type]: response };
      this.propagateChanges();

      return response;
    });

    CustomizationActions.get.promise(promise);
  },

  update(type: string, config: {}) {
    const promise = fetch('PUT', this._url(`/${type}`), config);

    promise.then(
      (response) => {
        this.customization = { ...this.customization, [type]: response };
        this.propagateChanges();

        return response;
      },
      (error) => {
        UserNotification.error(`Update failed: ${error}`, `Could not update customization: ${type}`);
      },
    );

    CustomizationActions.update.promise(promise);
  },

  propagateChanges() {
    this.trigger(this.getState());
  },

  getState(): CustomizationsStoreState {
    return {
      customization: this.customization,
    };
  },

  _url(path: string): string {
    return qualifyUrl(urlPrefix + path);
  },
}));

export default CustomizationStore;
