import jQuery from 'jquery';
import { configure } from 'enzyme';
import Adapter from 'enzyme-adapter-react-15';

global.$ = jQuery;
global.jQuery = jQuery;

configure({ adapter: new Adapter() });
