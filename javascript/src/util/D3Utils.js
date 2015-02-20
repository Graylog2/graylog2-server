'use strict';

var d3 = require('d3');

var D3Utils = {
    glColourPalette() {
        return d3.scale.ordinal().range(['#16ACE3', '#6DCFF6', '#F7941E', '#FBB040', '#BE1E2D',
                                         '#FF3B00', '#333333', '#9E1F63', '#8DC63F', '#BFD730',
                                         '#D7DF23', '#E3E5E5', '#F1F2F2']);
    }
};

module.exports = D3Utils;