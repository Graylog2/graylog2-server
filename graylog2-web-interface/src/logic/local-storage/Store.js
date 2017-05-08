const localStorage = window.localStorage;

const Store = {
  set(key, value) {
    localStorage.setItem(key, JSON.stringify(value));
  },

  get(key) {
    const value = localStorage.getItem(key);
    if (value === undefined || value === null) {
      return undefined;
    }

    try {
      return JSON.parse(value);
    } catch (e) {
      return value;
    }
  },

  delete(key) {
    localStorage.removeItem(key);
  },
};

export default Store;
