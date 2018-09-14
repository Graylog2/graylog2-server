class CombinedProvider {
  constructor() {
    this.actions = {};
    this.stores = {};
  }

  registerAction(key, func) {
    if (this.actions[key]) {
      throw new Error(`Unable to register actions for '${key}', already registered: ${this.actions[key]}`);
    }
    this.actions[key] = func;
  }

  registerStore(key, func) {
    if (this.stores[key]) {
      throw new Error(`Unable to register store for '${key}', already registered: ${this.stores[key]}`);
    }
    this.stores[key] = func;
  }

  get(name) {
    const result = {};
    if (this.stores[name]) {
      result[`${name}Store`] = this.stores[name]();
    }
    if (this.actions[name]) {
      result[`${name}Actions`] = this.actions[name]();
    }
    return result;
  }
}

if (typeof window.combinedProvider === 'undefined') {
  window.combinedProvider = new CombinedProvider();
}

export default window.combinedProvider;
