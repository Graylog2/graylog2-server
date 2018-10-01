import { format } from 'util';

// eslint-disable-next-line no-console
console.warn = jest.fn((...args) => {
  throw new Error(format(...args));
});

console.error = jest.fn((...args) => {
  throw new Error(format(...args));
});
