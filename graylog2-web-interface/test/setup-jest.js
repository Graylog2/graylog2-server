import jQuery from 'jquery';
import registerBuiltinStores from 'injection/registerBuiltinStores';
import sizeMe from 'react-sizeme';

global.$ = jQuery;
global.jQuery = jQuery;
sizeMe.noPlaceholders = true;

registerBuiltinStores();
