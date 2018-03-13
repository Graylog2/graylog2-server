import jQuery from 'jquery';

global.$ = jQuery;
global.jQuery = jQuery;

import { configure } from 'enzyme';
import Adapter from 'enzyme-adapter-react-15';

configure({ adapter: new Adapter() });

