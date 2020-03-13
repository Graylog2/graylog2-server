// @flow strict
type MockMethod = string | [string, Function];

export default function (...args: Array<MockMethod>) {
  const store = {
    // eslint-disable-next-line func-call-spacing,no-spaced-func
    listen: jest.fn<[], () => void>(() => () => {}),
    getInitialState: jest.fn<[], void>(),
  };
  Array.from(args).forEach((method) => {
    if (Array.isArray(method)) {
      const [name, fn] = method;
      store[name] = fn;
    } else {
      store[method] = jest.fn<[], void>();
    }
  });

  return store;
}
