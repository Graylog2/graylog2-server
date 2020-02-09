/* eslint-disable no-console */
import jQuery from 'jquery';
import { configure } from 'wrappedEnzyme';
import Adapter from 'enzyme-adapter-react-16';
import registerBuiltinStores from 'injection/registerBuiltinStores';
import { compact } from 'lodash';
import { format } from 'util';
import { DEPRECATION_NOTICE } from 'util/constants';

global.$ = jQuery;
global.jQuery = jQuery;

registerBuiltinStores();

configure({ adapter: new Adapter() });

const ignoredMessages = [
  'react-async-component-lifecycle-hooks',
  DEPRECATION_NOTICE,
];

const oldConsoleWarn = console.warn;
const oldConsoleError = console.error;

const ignoreJestError = (args) => {
  if (args[0]) {
    return compact(ignoredMessages.map(ignoredMessage => args[0].includes(ignoredMessage)))[0];
  }

  return false;
};

// eslint-disable-next-line no-console
console.warn = jest.fn((...args) => {
  if (ignoreJestError(args)) {
    oldConsoleWarn(...args);
  } else {
    throw new Error(format(...args));
  }
});

console.error = jest.fn((...args) => {
  if (ignoreJestError(args)) {
    oldConsoleError(...args);
  } else {
    throw new Error(format(...args));
  }
});
