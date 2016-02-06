import pjson from '../../package.json';

class Version {
  constructor() {
    this.full = pjson.version;
    const splitVersion = this.full.split('.');
    this.major = splitVersion[0];
    this.minor = splitVersion[1];
  }

  getMajorAndMinorVersion() {
    return `${this.major}.${this.minor}`;
  }

  getFullVersion() {
    return this.full;
  }
}

const version = new Version();
export default version;
