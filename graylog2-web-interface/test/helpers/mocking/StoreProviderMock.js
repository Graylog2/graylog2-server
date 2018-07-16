export default class StoreProviderMock {
  constructor(stores = {}) {
    this.stores = stores;
  }

  getStore(name) {
    const result = this.stores[name] || {};
    if (!result[`${name}Store`]) {
      result[`${name}Store`] = {};
    }

    return result;
  }
}
