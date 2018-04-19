const ObjectUtils = {
  clone(object) {
    return JSON.parse(JSON.stringify(object));
  },

  isEmpty(object) {
    const keys = Object.keys(object);
    return keys && keys.length === 0;
  },

  getValue(object, path) {
    let realPath = [];
    if (typeof path === 'string') {
      realPath = path.split('.');
    }
    if (Array.isArray(path)) {
      realPath = path;
    }
    return realPath.reduce((obj, key) => {
      if (!obj) {
        return undefined;
      }
      return obj[key];
    }, object);
  },
};

export default ObjectUtils;
