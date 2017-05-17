const ObjectUtils = {
  clone(object) {
    return JSON.parse(JSON.stringify(object));
  },

  isEmpty(object) {
    const keys = Object.keys(object);
    return keys && keys.length === 0;
  },

  isShallowEqual(a, b) {
    const aProps = Object.getOwnPropertyNames(a);
    const bProps = Object.getOwnPropertyNames(b);

    if (aProps.length !== bProps.length) {
      return false;
    }

    for (let i = 0; i < aProps.length; i += 1) {
      const propName = aProps[i];
      if (a[propName] !== b[propName]) {
        return false;
      }
    }
    return true;
  },
};

export default ObjectUtils;
