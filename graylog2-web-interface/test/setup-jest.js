import jQuery from 'jquery';
import registerBuiltinStores from 'injection/registerBuiltinStores';

import '../config';

global.$ = jQuery;
global.jQuery = jQuery;

registerBuiltinStores();
