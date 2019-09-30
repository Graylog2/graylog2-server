import jQuery from 'jquery';
import { configure } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import registerBuiltinStores from 'injection/registerBuiltinStores';
import { format } from 'util';

global.$ = jQuery;
global.jQuery = jQuery;

registerBuiltinStores();

configure({ adapter: new Adapter() });

// eslint-disable-next-line no-console
console.error = jest.fn((...args) => {
  throw new Error(format(...args));
});
