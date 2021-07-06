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
import Qs from 'qs';
import URI from 'urijs';
import UAParser from 'ua-parser-js';

import AppConfig from 'util/AppConfig';

const ACCEPTED_PROTOCOLS = ['http:', 'https:'];

const qualifyRelativeURLWithCurrentHostname = (url: string): string => {
  const uri = new URI(url);

  if (!uri.hostname() || !uri.scheme()) {
    const currentLocation = new URI(window.location);

    return uri.absoluteTo(currentLocation).toString();
  }

  return url;
};

const URLUtils = {
  parser: new UAParser(),
  qualifyUrl(url: string) {
    const parsedUrl = new URI(url);

    if (parsedUrl.is('absolute')) {
      return url;
    }

    const absoluteServerUrl = qualifyRelativeURLWithCurrentHostname(AppConfig.gl2ServerUrl() ?? '');

    return new URI(absoluteServerUrl + url).normalizePathname().toString();
  },
  appPrefixed(url: string) {
    return URLUtils.concatURLPath(AppConfig.gl2AppPathPrefix(), url);
  },
  getParsedSearch(location: Location) {
    let search = {};
    let query = location.search;

    if (query) {
      if (query.indexOf('?') === 0 && query.length > 1) {
        query = query.substr(1, query.length - 1);
        search = Qs.parse(query);
      }
    }

    return search;
  },
  getParsedHash(location: Location) {
    let result = {};
    let { hash } = location;

    if (hash) {
      if (hash.indexOf('#') === 0 && hash.length > 1) {
        hash = hash.substr(1, hash.length - 1);
        result = Qs.parse(hash);
      }
    }

    return result;
  },
  replaceHashParam(name: string, newValue: string) {
    const origHash = this.getParsedHash(window.location);

    origHash[name] = newValue;
    window.location.replace(`#${Qs.stringify(origHash)}`);
  },
  concatURLPath(...allArgs: string[]) {
    const args = Array(allArgs.length);

    // eslint-disable-next-line no-plusplus
    for (let i = 0; i < allArgs.length; i++) {
      args[i] = allArgs[i];
    }

    const joinedPath = `/${args.join('/')}`;

    return joinedPath.replace(/[/]+/g, '/');
  },
  areCredentialsInURLSupported(): boolean {
    const browser = URLUtils.parser.getBrowser();

    return browser.name !== 'IE' && browser.name !== 'Edge';
  },
  isValidURL(str: string) {
    let isValid = true;

    try {
      // eslint-disable-next-line
      new URL(str);
    } catch (e) {
      isValid = false;
    }

    return isValid;
  },

  hasAcceptedProtocol(string: string, acceptedProtocols = ACCEPTED_PROTOCOLS) {
    const url = new URL(string);

    return acceptedProtocols.includes(url.protocol);
  },
};

export const qualifyUrlWithSessionCredentials = (url: string, sessionId: string) => {
  let result = new URI(URLUtils.qualifyUrl(url));

  if (URLUtils.areCredentialsInURLSupported()) {
    const trimmedSessionId = sessionId !== undefined ? sessionId : '';

    result = result.username(trimmedSessionId)
      .password('session');
  }

  return result.toString();
};

export default URLUtils;

export const {
  parser,
  appPrefixed,
  getParsedHash,
  getParsedSearch,
  qualifyUrl,
  replaceHashParam,
  concatURLPath,
  areCredentialsInURLSupported,
  isValidURL,
  hasAcceptedProtocol,
} = URLUtils;
