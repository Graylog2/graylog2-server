'use strict';

var $ = require('jquery');

var UIUtils = {
    NAVBAR_HEIGHT: 55,
    scrollToHint(element) {
        if (!this.isElementVisible(element)) {
            var $scrollHint = $("#scroll-to-hint");
            $scrollHint
                .fadeIn("fast")
                .delay(1500)
                .fadeOut("fast")
                .on('click', (event) => {
                    event.preventDefault();
                    var top = window.pageYOffset - this.NAVBAR_HEIGHT + element.getBoundingClientRect().top;
                    $("html, body").animate({ scrollTop: top }, 'fast');
                    $scrollHint.off('click');
                });
        }
    },
    isElementVisible(element) {
        var rect = element.getBoundingClientRect();

        return rect.top > 0 && rect.bottom > 0;
    }
};

module.exports = UIUtils;