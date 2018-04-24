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
      const arrayMatch = key.match(/^(\w*)\[(\d*)\]/);
      if (arrayMatch) {
        const realKey = arrayMatch[1];
        const index = arrayMatch[2];
        return obj[realKey][index];
      }
      return obj[key];
    }, object);
  },

  setValue(object, path, newValue) {
    let realPath = [];
    if (typeof path === 'string') {
      realPath = path.split('.');
    }
    if (Array.isArray(path)) {
      realPath = path;
    }
    let obj = object;
    const lastKey = realPath.pop();
    realPath.forEach((key) => {
      const arrayMatch = key.match(/^(\w*)\[(\d*)\]/);
      if (arrayMatch) {
        const realKey = arrayMatch[1];
        const index = arrayMatch[2];
        obj = obj[realKey][index];
        return;
      }
      obj = obj[key];
    });
    const arrayMatch = lastKey.match(/^(\w*)\[(\d*)\]/);
    if (arrayMatch) {
      const realKey = arrayMatch[1];
      const index = arrayMatch[2];
      obj[realKey][index] = newValue;
      return;
    }
    obj[lastKey] = newValue;
  },

  getPaths(config, parentKeys = '') {
    if (!config || typeof config !== 'object') {
      return parentKeys;
    }
    const keys = Object.keys(config);
    if (!keys || keys.length <= 0) {
      return parentKeys;
    }
    const result = keys.map((key) => {
      let newKeys = parentKeys.repeat(1);
      const newKey = newKeys.length <= 0 ? key : `.${key}`;
      const value = ObjectUtils.getValue(config, key);
      if (typeof value === 'object' && !Array.isArray(value)) {
        newKeys = newKeys.concat(newKey);
        return ObjectUtils.getPaths(value, newKeys);
      } else if (typeof value === 'object' && Array.isArray(value)) {
        const arrayKeys = value.map((item, index) => {
          const newArrayKey = `${newKey}[${index}]`;
          const newArrayKeys = newKeys.concat(newArrayKey);
          return ObjectUtils.getPaths(item, newArrayKeys);
        });
        return [].concat(...arrayKeys);
      } else {
        newKeys = newKeys.concat(newKey);
        return newKeys;
      }
    });
    return [].concat(...result);
  },

};

export default ObjectUtils;
