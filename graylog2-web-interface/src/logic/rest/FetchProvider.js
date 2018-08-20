import request from 'superagent-bluebird-promise';
import BluebirdPromise from 'bluebird';

import StoreProvider from 'injection/StoreProvider';

import ActionsProvider from 'injection/ActionsProvider';

import Routes from 'routing/Routes';
import history from 'util/History';

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
        if (SessionStore.isLoggedIn() && error.status === 401) {
          const SessionActions = ActionsProvider.getActions('Session');
          SessionActions.logout(SessionStore.getSessionId());
        }

        // Redirect to the start page if a user is logged in but not allowed to access a certain HTTP API.
        if (SessionStore.isLoggedIn() && error.status === 403) {
          history.replace(Routes.NOTFOUND);
        }

        if (error.originalError && !error.originalError.status) {
          const ServerAvailabilityActions = ActionsProvider.getActions('ServerAvailability');
          ServerAvailabilityActions.reportError(error);
        }

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
        if (SessionStore.isLoggedIn() && error.status === 401) {
          const SessionActions = ActionsProvider.getActions('Session');
          SessionActions.logout(SessionStore.getSessionId());
        }

        // Redirect to the start page if a user is logged in but not allowed to access a certain HTTP API.
        if (SessionStore.isLoggedIn() && error.status === 403) {
          history.replace(Routes.STARTPAGE);
        }

        if (error.originalError && !error.originalError.status) {
          const ServerAvailabilityActions = ActionsProvider.getActions('ServerAvailability');
          ServerAvailabilityActions.reportError(error);
        }

        throw new FetchError(error.statusText, error);
      });

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
