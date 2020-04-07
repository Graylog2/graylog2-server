import jQuery from 'jquery';
import registerBuiltinStores from 'injection/registerBuiltinStores';
import './console-warnings-fail-tests';

global.$ = jQuery;
global.jQuery = jQuery;

registerBuiltinStores();
