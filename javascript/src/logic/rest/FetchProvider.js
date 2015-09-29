import ifetch from 'isomorphic-fetch';
import SessionStore from 'stores/sessions/SessionStore';

const createBasicAuthHeader = (username, password) => {
  return 'Basic ' + btoa(username + ':' + password);
};
export function fetch(url, options) {
  if (options === undefined) {
    options = {headers: {}};
  }
  if (options.headers === undefined) {
    options.headers = {};
  }

  const token = SessionStore.getSessionId();

  options.headers.Authorization = createBasicAuthHeader(token, 'session');
  return ifetch(url, options);
}

export function fetchUnauthenticated(url, options) {
  return ifetch(url, options);
}

export function fetchJson(url, options, body) {
  if (options === undefined) {
    options = {headers: {}};
  }
  if (options.headers === undefined) {
    options.headers = {};
  }

  options.headers.Accept = 'application/json';
  options.headers['Content-Type'] = 'application/json';

  if (body) {
    options.body = JSON.stringify(body);
  }

  return fetch(url, options).then((response) => {
    return response.json();
  });
}
