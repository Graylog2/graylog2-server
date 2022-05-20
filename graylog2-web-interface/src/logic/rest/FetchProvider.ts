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
import FetchError from 'logic/errors/FetchError';
import ErrorsActions from 'actions/errors/ErrorsActions';
import { createFromFetchError } from 'logic/errors/ReportedErrors';
import Routes from 'routing/Routes';
import history from 'util/History';
import CancellablePromise from 'logic/rest/CancellablePromise';
import { ServerAvailabilityActions } from 'stores/sessions/ServerAvailabilityStore';

const reportServerSuccess = () => {
  ServerAvailabilityActions.reportSuccess();
};

const defaultOnUnauthorizedError = (error) => ErrorsActions.report(createFromFetchError(error));

const emptyToUndefined = (s: any) => (s && s !== '' ? s : undefined);

const onServerError = async (error: Response | undefined, onUnauthorized = defaultOnUnauthorizedError) => {
  const contentType = error.headers?.get('Content-Type');
  const response = await (contentType?.startsWith('application/json') ? error.json().then((body) => body) : error?.text?.());
  // eslint-disable-next-line global-require
  const { SessionStore, SessionActions } = require('stores/sessions/SessionStore');
  const fetchError = new FetchError(error.statusText, error.status, emptyToUndefined(response));

  if (SessionStore.isLoggedIn() && error.status === 401) {
    SessionActions.logout();
  }

  // Redirect to the start page if a user is logged in but not allowed to access a certain HTTP API.
  if (SessionStore.isLoggedIn() && error.status === 403) {
    onUnauthorized(fetchError);
  }

  if (error && !error.status) {
    ServerAvailabilityActions.reportError(fetchError);
  }

  throw fetchError;
};

const maybeStringify = (body: any) => (body && typeof body !== 'string' ? JSON.stringify(body) : body);

type RequestHeaders = {
  Accept?: string,
  'Content-Type'?: string,
};

const defaultResponseHandler = (resp: Response) => {
  if (resp.ok) {
    const { status } = resp;
    const contentLength = Number.parseInt(resp.headers.get('Content-Length'), 10);
    const noContent = status === 204 || contentLength === 0;

    reportServerSuccess();

    return noContent ? null : resp.json();
  }

  throw resp;
};

export class Builder {
  private options = {};

  private readonly url: string;

  private readonly method: string;

  private body: { body: any, mimeType?: string };

  private accept: string;

  private responseHandler: (response: any) => any;

  private errorHandler: (error: any) => any;

  constructor(method, url) {
    this.method = method;
    this.url = url.replace(/([^:])\/\//, '$1/');

    this.options = {
      'X-Requested-With': 'XMLHttpRequest',
      'X-Requested-By': 'XMLHttpRequest',
    };

    this.responseHandler = (response) => response;
    this.errorHandler = undefined;
  }

  setHeader(header, value) {
    this.options = {
      ...this.options,
      [header]: value,
    };

    return this;
  }

  json(body?: any) {
    this.body = { body: maybeStringify(body), mimeType: 'application/json' };
    this.accept = 'application/json';

    this.responseHandler = defaultResponseHandler;

    this.errorHandler = (error) => onServerError(error);

    return this;
  }

  formData(body, acceptedMimeType = 'application/json') {
    this.body = { body };

    this.accept = acceptedMimeType;

    this.responseHandler = defaultResponseHandler;
    this.errorHandler = (error) => onServerError(error);

    return this;
  }

  file(body, mimeType) {
    this.body = { body: maybeStringify(body), mimeType: 'application/json' };
    this.accept = mimeType;

    this.responseHandler = (resp) => {
      if (resp.ok) {
        reportServerSuccess();

        return resp.text();
      }

      throw resp;
    };

    this.errorHandler = (error) => onServerError(error);

    return this;
  }

  plaintext(body) {
    const onUnauthorized = () => history.replace(Routes.STARTPAGE);

    this.body = { body, mimeType: 'text/plain' };
    this.accept = 'application/json';

    this.responseHandler = defaultResponseHandler;

    this.errorHandler = (error) => onServerError(error, onUnauthorized);

    return this;
  }

  ignoreUnauthorized() {
    this.errorHandler = (error) => onServerError(error, () => {});

    return this;
  }

  noSessionExtension() {
    this.options = {
      ...this.options,
      'X-Graylog-No-Session-Extension': 'true',
    };

    return this;
  }

  build() {
    const headers: RequestHeaders = this.body && this.body.mimeType
      ? { ...this.options, 'Content-Type': this.body.mimeType }
      : this.options;

    if (this.accept) {
      headers.Accept = this.accept;
    }

    return CancellablePromise.of(window.fetch(this.url, {
      method: this.method,
      headers,
      body: this.body ? this.body.body : undefined,
    })).then(this.responseHandler, this.errorHandler)
      .catch(this.errorHandler);
  }
}

function queuePromiseIfNotLoggedin(promise) {
  // eslint-disable-next-line global-require
  const { SessionStore, SessionActions } = require('stores/sessions/SessionStore');

  if (!SessionStore.isLoggedIn()) {
    return () => CancellablePromise.of(new Promise((resolve, reject) => {
      SessionActions.login.completed.listen(() => {
        promise().then(resolve, reject);
      });
    }));
  }

  return promise;
}

export default function fetch(method, url, body?) {
  const promise = () => new Builder(method, url)
    .json(body)
    .build();

  return queuePromiseIfNotLoggedin(promise)();
}

export function fetchPlainText(method, url, body) {
  const promise = () => new Builder(method, url)
    .plaintext(body)
    .build();

  return queuePromiseIfNotLoggedin(promise)();
}

export function fetchPeriodically(method, url, body?) {
  const promise = () => new Builder(method, url)
    .noSessionExtension()
    .json(body)
    .build();

  return queuePromiseIfNotLoggedin(promise)();
}

export function fetchFile(method, url, body, mimeType = 'text/csv') {
  const promise = () => new Builder(method, url)
    .file(body, mimeType)
    .build();

  return queuePromiseIfNotLoggedin(promise)();
}
