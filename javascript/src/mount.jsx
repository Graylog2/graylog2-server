/* global userPreferences */

'use strict';

var $ = require('jquery'); // excluded and shimed

$(document).ready(() => {
    require('./components/users/mount');
    require('./components/source-tagging/mount');
    require('./components/start-page/mount');
    require('./components/sources/mount');
    require('./components/dashboard/mount');
    if (userPreferences.enableSmartSearch) {
        require('./components/search/mount');
    }
});
