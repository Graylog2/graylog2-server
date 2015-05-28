'use strict';

var $ = require('jquery');

var UIUtils = {
    NAVBAR_HEIGHT: 50,
    scrollToHint(element) {
        if (!this.isElementVisible(element)) {
            $("#scroll-to-search-hint").fadeIn("fast").delay(1500).fadeOut("fast");
        }
    },
    isElementVisible(element) {
        var rect = element.getBoundingClientRect();

        return rect.top > this.NAVBAR_HEIGHT && rect.bottom > this.NAVBAR_HEIGHT;
    }
};

module.exports = UIUtils;