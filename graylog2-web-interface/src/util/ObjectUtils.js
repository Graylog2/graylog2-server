const ObjectUtils = {
  clone(object) {
    return JSON.parse(JSON.stringify(object));
  },

  isEmpty(object) {
    const keys = Object.keys(object);
    return keys && keys.length === 0;
  },
};

export default ObjectUtils;
