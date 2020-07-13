import jQuery from 'jquery';
import sizeMe from 'react-sizeme';

import registerBuiltinStores from 'injection/registerBuiltinStores';

global.$ = jQuery;

global.jQuery = jQuery;

sizeMe.noPlaceholders = true;

registerBuiltinStores();
