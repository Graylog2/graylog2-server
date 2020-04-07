import jQuery from 'jquery';
import registerBuiltinStores from 'injection/registerBuiltinStores';

global.$ = jQuery;
global.jQuery = jQuery;

registerBuiltinStores();
