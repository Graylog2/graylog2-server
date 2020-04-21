import request from 'superagent-bluebird-promise';
import BluebirdPromise from 'bluebird';

import ErrorsActions from 'actions/errors/ErrorsActions';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';
// eslint-disable-next-line import/no-cycle
import { createUnauthorizedError } from 'logic/errors/ReportedErrors';

import Routes from 'routing/Routes';
import history from 'util/History';

export const logoutIfUnauthorized = (error, SessionStore) => {
  if (SessionStore.isLoggedIn()) {
    const SessionActions = ActionsProvider.getActions('Session');
    SessionActions.logout(SessionStore.getSessionId());
  }
};

export const redirectIfForbidden = (error, SessionStore, route = Routes.NOTFOUND) => {
  if (SessionStore.isLoggedIn()) {
    history.replace(route);
  }
};

export const queuePromiseIfNotLoggedin = (promise) => {
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
};

const reportError = (error) => {
  if (error.originalError && !error.originalError.status) {
    const ServerAvailabilityActions = ActionsProvider.getActions('ServerAvailability');
    ServerAvailabilityActions.reportError(error);
  }
};

export class FetchError extends Error {
  constructor(message, additional) {
    super(message);
    this.message = message || (additional.message || 'Undefined error.');
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
    this.status = additional.status; // Shortcut, as this is often used
  }
}

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

  noSessionExtension() {
    this.request = this.request.set('X-Graylog-No-Session-Extension', 'true');

    return this;
  }

  json(body) {
    this.request = this.request
      .send(body)
      .type('json')
      .accept('json')
      .then((resp) => {
        if (resp.ok) {
          const ServerAvailabilityActions = ActionsProvider.getActions('ServerAvailability');
          ServerAvailabilityActions.reportSuccess();
          return resp.body;
        }
        throw new FetchError(resp.statusText, resp);
      }, (error) => {
        const SessionStore = StoreProvider.getStore('Session');

        if (error.status === 401) {
          this._handleUnauthorized(error, SessionStore);
        }
        if (error.status === 403) {
          this._handleForbidden(error, SessionStore);
        }

        reportError(error);

        throw new FetchError(error.statusText, error);
      });

    return this;
  }

  plaintext(body) {
    this.request = this.request
      .send(body)
      .type('text/plain')
      .accept('json')
      .then((resp) => {
        if (resp.ok) {
          const ServerAvailabilityActions = ActionsProvider.getActions('ServerAvailability');
          ServerAvailabilityActions.reportSuccess();
          return resp.body;
        }

        throw new FetchError(resp.statusText, resp);
      }, (error) => {
        const SessionStore = StoreProvider.getStore('Session');
        if (error.status === 401) {
          this._handleUnauthorized(error, SessionStore);
        }
        // Redirect to the start page if a user is logged in but not allowed to access a certain HTTP API.
        if (error.status === 403) {
          this._handleForbidden(error, SessionStore, Routes.STARTPAGE);
        }
        reportError(error);
        throw new FetchError(error.statusText, error);
      });

    return this;
  }

  _handleUnauthorized(error, SessionStore) {
    logoutIfUnauthorized(error, SessionStore);
    return this;
  }

  _handleForbidden(error, SessionStore, route) {
    redirectIfForbidden(error, SessionStore, route);
    return this;
  }

  handleUnauthorized(fn) {
    this._handleUnauthorized = fn;
    return this;
  }

  handleForbidden(fn) {
    this._handleForbidden = fn;
    return this;
  }


  build() {
    return this.request;
  }
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
