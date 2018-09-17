import jQuery from 'jquery';
import { configure } from 'enzyme';
import Adapter from 'enzyme-adapter-react-15';
import registerBuiltinStores from 'injection/registerBuiltinStores';

global.$ = jQuery;
global.jQuery = jQuery;

registerBuiltinStores();

configure({ adapter: new Adapter() });
