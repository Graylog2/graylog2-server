export default class CombinedProviderMock {
  static defaultStore = {
    get: () => jest.fn(() => ({})),
    listen: jest.fn(),
  };

  static defaultActions = {};

  constructor(rules = {}, defaultStore = CombinedProviderMock.defaultStore, defaultActions = CombinedProviderMock.defaultActions) {
    this.rules = rules;
    this.store = defaultStore;
    this.actions = defaultActions;
  }

  get(name) {
    const result = this.rules[name] || {};
    if (!result[`${name}Store`]) {
      result[`${name}Store`] = this.store;
    }
    if (!result[`${name}Actions`]) {
      result[`${name}Actions`] = this.actions;
    }

    return result;
  }
}
