const namedProxy = name => new Proxy({}, {
  get: function getter(target, key) {
    if (key === '__esModule') {
      return false;
    }
    if (key === 'toString' || key === Symbol.toPrimitive) {
      return () => name;
    }
    return namedProxy(`${name}.${key.toString()}`);
  },
});

export default new Proxy({}, {
  get: function getter(target, key) {
    if (key === '__esModule') {
      return false;
    }
    if (key === 'use' || key === 'unuse') {
      return () => {};
    }
    return namedProxy(key);
  },
});
