export default function() {
  const store = {};
  Array.from(arguments).forEach((method) => { store[method] = jest.fn(); });

  return store;
}
