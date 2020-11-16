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
import request from 'superagent-bluebird-promise';
import BluebirdPromise from 'bluebird';

import ErrorsActions from 'actions/errors/ErrorsActions';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';
// eslint-disable-next-line import/no-cycle
import { createFromFetchError } from 'logic/errors/ReportedErrors';
import Routes from 'routing/Routes';
import history from 'util/History';

export class FetchError extends Error {
  constructor(message, additional) {
    super(message);
    this.message = message ?? additional?.message ?? 'Undefined error.';

    /* eslint-disable no-console */
    try {
      this.responseMessage = additional.body ? additional.body.message : undefined;

      console.error(`There was an error fetching a resource: ${this.message}.`,
        `Additional information: ${additional.body && additional.body.message ? additional.body.message : 'Not available'}`);
    } catch (e) {
      console.error(`There was an error fetching a resource: ${this.message}. No additional information available.`);
    }
    /* eslint-enable no-console */

    this.additional = additional;
    this.status = additional?.status; // Shortcut, as this is often used
  }
}

const reportServerSuccess = () => {
  const ServerAvailabilityActions = ActionsProvider.getActions('ServerAvailability');

  ServerAvailabilityActions.reportSuccess();
};

const defaultOnUnauthorizedError = (error) => ErrorsActions.report(createFromFetchError(error));

const onServerError = (error, onUnauthorized = defaultOnUnauthorizedError) => {
  const SessionStore = StoreProvider.getStore('Session');
  const fetchError = new FetchError(error.statusText, error);

  if (SessionStore.isLoggedIn() && error.status === 401) {
    const SessionActions = ActionsProvider.getActions('Session');

    SessionActions.logout(SessionStore.getSessionId());
  }

  // Redirect to the start page if a user is logged in but not allowed to access a certain HTTP API.
  if (SessionStore.isLoggedIn() && error.status === 403) {
    onUnauthorized(fetchError);
  }

  if (error.originalError && !error.originalError.status) {
    const ServerAvailabilityActions = ActionsProvider.getActions('ServerAvailability');

    ServerAvailabilityActions.reportError(fetchError);
  }

  throw fetchError;
};

export class Builder {
  constructor(method, url) {
    this.request = request(method, url.replace(/([^:])\/\//, '$1/'))
      .set('X-Requested-With', 'XMLHttpRequest')
      .set('X-Requested-By', 'XMLHttpRequest');
  }

  authenticated() {
    const SessionStore = StoreProvider.getStore('Session');
    const token = SessionStore.getSessionId();

    return this.session(token);
  }

  session(sessionId) {
    this.request = this.request.auth(sessionId, 'session');

    return this;
  }

  setHeader(header, value) {
    this.request = this.request.set(header, value);

    return this;
  }

  json(body) {
    this.request = this.request
      .send(body)
      .type('json')
      .accept('json')
      .then((resp) => {
        if (resp.ok) {
          reportServerSuccess();

          return resp.body;
        }

        throw new FetchError(resp.statusText, resp);
      }, (error) => onServerError(error));

    return this;
  }

  file(body, mimeType) {
    this.request = this.request
      .send(body)
      .type('json')
      .accept(mimeType)
      .then((resp) => {
        if (resp.ok) {
          reportServerSuccess();

          return resp.text;
        }

        throw new FetchError(resp.statusText, resp);
      }, (error) => onServerError(error));

    return this;
  }

  plaintext(body) {
    const onUnauthorized = () => history.replace(Routes.STARTPAGE);

    this.request = this.request
      .send(body)
      .type('text/plain')
      .accept('json')
      .then((resp) => {
        if (resp.ok) {
          reportServerSuccess();

          return resp.body;
        }

        throw new FetchError(resp.statusText, resp);
      }, (error) => onServerError(error, onUnauthorized));

    return this;
  }

  noSessionExtension() {
    this.request = this.request.set('X-Graylog-No-Session-Extension', 'true');

    return this;
  }

  build() {
    return this.request;
  }
}

function queuePromiseIfNotLoggedin(promise) {
  const SessionStore = StoreProvider.getStore('Session');

  if (!SessionStore.isLoggedIn()) {
    return () => new BluebirdPromise((resolve, reject) => {
      const SessionActions = ActionsProvider.getActions('Session');

      SessionActions.login.completed.listen(() => {
        promise().then(resolve, reject);
      });
    });
  }

  return promise;
}

export default function fetch(method, url, body) {
  const promise = () => new Builder(method, url)
    .authenticated()
    .json(body)
    .build();

  return queuePromiseIfNotLoggedin(promise)();
}

export function fetchPlainText(method, url, body) {
  const promise = () => new Builder(method, url)
    .authenticated()
    .plaintext(body)
    .build();

  return queuePromiseIfNotLoggedin(promise)();
}

export function fetchPeriodically(method, url, body) {
  const promise = () => new Builder(method, url)
    .authenticated()
    .noSessionExtension()
    .json(body)
    .build();

  return queuePromiseIfNotLoggedin(promise)();
}

export function fetchFile(method, url, body, mimeType = 'text/csv') {
  const promise = () => new Builder(method, url)
    .authenticated()
    .file(body, mimeType)
    .build();

  return queuePromiseIfNotLoggedin(promise)();
}
