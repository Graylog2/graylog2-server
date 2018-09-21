import jQuery from 'jquery';
import { configure } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import registerBuiltinStores from 'injection/registerBuiltinStores';

global.$ = jQuery;
global.jQuery = jQuery;

registerBuiltinStores();

configure({ adapter: new Adapter() });

console.error = jest.fn((error) => {
  throw new Error(error);
});
