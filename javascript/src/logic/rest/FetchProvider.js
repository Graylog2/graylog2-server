import request from 'superagent-bluebird-promise';
import SessionStore from 'stores/sessions/SessionStore';
import SessionActions from 'actions/sessions/SessionActions';

export class FetchError extends Error {
  constructor(message, additional) {
    super(message);
    this.message = message ? message : additional.message;
    this.additional = additional;
  }
}

export class Builder {
  constructor(method, url) {
    this.request = request(method, url);
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
          return resp.body;
        }

        throw new FetchError(resp.statusText, resp);
      }, (error) => {
        if (error.status === 401) {
          SessionActions.logout(SessionStore.getSessionId());
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
  const builder = new Builder(method, url)
    .authenticated()
    .json(body);

  return builder.build();
}
