import CombinedProvider from 'injection/CombinedProvider';

/* global storeProvider */

class StoreProvider {
  getStore(storeName) {
    const result = CombinedProvider.get(storeName);
    if (!result[`${storeName}Store`]) {
      throw new Error(`Requested store "${storeName}" is not registered.`);
    }
    return result[`${storeName}Store`];
  }
}

if (typeof storeProvider === 'undefined') {
  window.storeProvider = new StoreProvider();
}

export default storeProvider;
