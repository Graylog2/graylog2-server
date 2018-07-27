export default class StoreProviderMock {
  static defaultStore = {
    get: () => jest.fn(() => ({})),
    listen: jest.fn(),
  };

  constructor(stores = StoreProviderMock.defaultStore) {
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
