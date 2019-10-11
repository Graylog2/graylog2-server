// @flow strict

const singleton = (key: string, supplier: () => any) => {
  if (!window.singletons[key]) {
    window.singletons[key] = supplier();
  }
  return window.singletons[key];
};

const singletonActions = (key: string, supplier: () => any) => singleton(`${key}Actions`, supplier);

const singletonStore = (key: string, supplier: () => any) => singleton(`${key}Store`, supplier);

if (typeof window.singletons === 'undefined') {
  window.singletons = {};
}

export {
  singletonActions,
  singletonStore,
};
