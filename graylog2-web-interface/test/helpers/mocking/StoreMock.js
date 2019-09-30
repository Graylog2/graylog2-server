export default function (...args) {
  const store = {};
  Array.from(args).forEach((method) => {
    if (Array.isArray(method)) {
      // eslint-disable-next-line prefer-destructuring
      store[method[0]] = method[1];
    } else {
      store[method] = jest.fn();
    }
  });

  return store;
}
