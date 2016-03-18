import request from 'superagent-bluebird-promise';
import SessionStore from 'stores/sessions/SessionStore';
import SessionActions from 'actions/sessions/SessionActions';
import ServerAvailabilityActions from 'actions/sessions/ServerAvailabilityActions';
import Routes from 'routing/Routes';
import history from 'util/History';

export class FetchError extends Error {
  constructor(message, additional) {
    super(message);
    this.message = message ? message : (additional.message ? additional.message : 'Undefined error.');
    try {
      console.error(`There was an error fetching a resource: ${this.message}.`,
        `Additional information: ${additional.body && additional.body.message ? additional.body.message : 'Not available'}`);
    } catch (e) {
      console.error(`There was an error fetching a resource: ${this.message}. No additional information available.`);
    }

    this.additional = additional;
  }
}

export class Builder {
  constructor(method, url) {
    this.request = request(method, url.replace(/([^:])\/\//, '$1/'));
  }

  authenticated() {
    const token = SessionStore.getSessionId();
    this.request = this.request.auth(token, 'session');

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
          ServerAvailabilityActions.reportSuccess();
          return resp.body;
        }

        throw new FetchError(resp.statusText, resp);
      }, (error) => {
        if (SessionStore.isLoggedIn() && error.status === 401) {
          SessionActions.logout(SessionStore.getSessionId());
        }

        // Redirect to the start page if a user is logged in but not allowed to access a certain HTTP API.
        if (SessionStore.isLoggedIn() && error.status === 403) {
          history.replaceState(null, Routes.STARTPAGE);
        }

        if (error.originalError && !error.originalError.status) {
          ServerAvailabilityActions.reportError(error);
        }

        throw new FetchError(error.statusText, error);
      });

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

  if (!SessionStore.isLoggedIn()) {
    return new Promise((resolve, reject) => {
      SessionActions.login.completed.listen(() => {
        promise().then(resolve, reject);
      });
    });
  }
  return promise();
}
