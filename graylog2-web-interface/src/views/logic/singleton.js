// @flow strict

// eslint-disable-next-line arrow-parens
const singleton = <R>(key: string, supplier: () => R): R => {
  if (!window.singletons[key]) {
    window.singletons[key] = supplier();
  }

  return window.singletons[key];
};

const singletonActions = <R>(key: string, supplier: () => R): R => singleton(`${key}Actions`, supplier);

const singletonStore = <R>(key: string, supplier: () => any): R => singleton(`${key}Store`, supplier);

if (typeof window.singletons === 'undefined') {
  window.singletons = {};
}

export {
  singleton,
  singletonActions,
  singletonStore,
};
