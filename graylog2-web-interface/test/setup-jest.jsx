/* eslint-disable no-console */
import jQuery from 'jquery';
import { configure } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import registerBuiltinStores from 'injection/registerBuiltinStores';
import { format } from 'util';

global.$ = jQuery;
global.jQuery = jQuery;

registerBuiltinStores();

configure({ adapter: new Adapter() });

const originalWarn = console.warn;

console.warn = jest.fn((...args) => {
  if (!args[0] || !args[0].includes('react-async-component-lifecycle-hooks')) {
    originalWarn(format(...args));
  }
});

console.error = jest.fn((...args) => {
  if (!args[0] || !args[0].includes('react-async-component-lifecycle-hooks')) {
    throw new Error(format(...args));
  }
});
