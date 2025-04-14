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
import { singletonStore, singletonActions } from 'logic/singleton';

type ServerAvailabilityActionsType = {
  reportError: (error: any) => void;
  reportSuccess: () => void;
};
export const ServerAvailabilityActions = singletonActions('core.ServerAvailability', () =>
  Reflux.createActions(['reportError', 'reportSuccess']),
) as unknown as ServerAvailabilityActionsType;

export type ServerError = {
  message: string;
  originalError: {
    method: string;
    url: string;
    status: number;
  };
};
export type ServerAvailabilityStoreState = {
  server: { up: true } | { up: false; error: ServerError };
  version: string;
};

const ping = (url: string) =>
  window
    .fetch(url, {
      method: 'GET',
      headers: {
        Accept: 'application/json',
        'X-Graylog-No-Session-Extension': 'true',
      },
    })
    .then<StatusResponse>((response) => response.json());

type StatusResponse = {
  version: string;
};
export const ServerAvailabilityStore = singletonStore('core.ServerAvailability', () =>
  Reflux.createStore<ServerAvailabilityStoreState>({
    listenables: [ServerAvailabilityActions],
    server: { up: true },
    version: undefined,
    init() {
      this.ping();
    },
    getInitialState() {
      return { server: this.server, version: this.version };
    },
    ping() {
      return ping(URLUtils.qualifyUrl(ApiRoutes.ping().url)).then(
        (response: StatusResponse) => {
          if (response?.version !== this.version) {
            this.version = response?.version;
            this._trigger();
          }

          return ServerAvailabilityActions.reportSuccess();
        },
        (error) => ServerAvailabilityActions.reportError(error),
      );
    },
    reportError(error) {
      if (this.server.up) {
        this.server = { up: false, error: error };
        this._trigger();
      }
    },
    reportSuccess() {
      if (!this.server.up) {
        this.server = { up: true };
        this._trigger();
      }
    },
    _trigger() {
      this.trigger({ server: this.server, version: this.version });
    },
  }),
);
