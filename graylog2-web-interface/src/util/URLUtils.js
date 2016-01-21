import Qs from 'qs';
import AppConfig from 'util/AppConfig';

const appConfig = new AppConfig();

const URLUtils = {
  qualifyUrl(url) {
    return appConfig.gl2ServerUrl() + this.appPrefixed(url);
  },
  appPrefixed(url) {
    return this.concatURLPath(appConfig.gl2AppPathPrefix(), url);
  },
  openLink(url, newWindow) {
    if (newWindow) {
      window.open(url);
    } else {
      window.location = url;
    }
  },
  getParsedSearch(location) {
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
  getParsedHash(location) {
    let result = {};
    let hash = location.hash;
    if (hash) {
      if (hash.indexOf('#') === 0 && hash.length > 1) {
        hash = hash.substr(1, hash.length - 1);
        result = Qs.parse(hash);
      }
    }
    return result;
  },
  replaceHashParam(name, newValue) {
    const origHash = this.getParsedHash(window.location);
    origHash[name] = newValue;
    window.location.replace(`#${Qs.stringify(origHash)}`);
  },
  concatURLPath() {
    const args = Array(arguments.length);
    for (let i = 0; i < arguments.length; i++) {
      args[i] = arguments[i];
    }

    const joinedPath = '/' + args.join('/');
    return joinedPath.replace(/[\/]+/g, '/');
  },
};

export default URLUtils;
