import CombinedProvider from 'injection/CombinedProvider';

class StoreProvider {
  getStore(storeName) {
    const result = CombinedProvider.get(storeName);
    if (!result[`${storeName}Store`]) {
      throw new Error(`Requested store "${storeName}" is not registered.`);
    }
    return result[`${storeName}Store`];
  }
}

if (typeof window.storeProvider === 'undefined') {
  window.storeProvider = new StoreProvider();
}

export default window.storeProvider;
