import lodash from 'lodash';

const SidecarStatusEnum = {
  RUNNING: 0,
  UNKNOWN: 1,
  FAILING: 2,
  STOPPED: 3,
  properties: {
    0: { name: 'running' },
    1: { name: 'unknown' },
    2: { name: 'failing' },
    3: { name: 'stopped' },
  },

  isValidStatusCode(statusCode) {
    return Object.keys(this.properties).includes(String(statusCode));
  },

  toStatusCode(stringStatus) {
    const status = lodash.lowerCase(stringStatus);
    if (status === this.properties[this.RUNNING].name) {
      return this.RUNNING;
    }
    if (status === this.properties[this.FAILING].name) {
      return this.FAILING;
    }
    if (status === this.properties[this.STOPPED].name) {
      return this.STOPPED;
    }
    return this.UNKNOWN;
  },

  toString(statusCode) {
    switch (Number(statusCode)) {
      case this.RUNNING:
        return 'running';
      case this.FAILING:
        return 'failing';
      case this.STOPPED:
        return 'stopped';
      default:
        return 'unknown';
    }
  },
};

export default SidecarStatusEnum;