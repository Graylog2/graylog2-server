import { format } from 'util';

/* eslint-disable no-console */

const oldConsoleWarn = console.warn;
const oldConsoleError = console.error;

const ignoredWarnings = [
  'react-async-component-lifecycle-hooks',
  'react-unsafe-component-lifecycles',
];

const ignoreWarning = args => (!args[0] || ignoredWarnings.filter(warning => args[0].includes(warning)).length > 0);

console.warn = jest.fn((...args) => {
  if (!ignoreWarning(args)) {
    throw new Error(format(...args));
  }
  oldConsoleWarn(...args);
});

console.error = jest.fn((...args) => {
  if (!ignoreWarning(args)) {
    throw new Error(format(...args));
  }
  oldConsoleError(...args);
});
