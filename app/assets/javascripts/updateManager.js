(function (exports) {
    'use strict';

    var focussed = true;

    $(window).blur(function () {
        $(".focuslimit").css("text-decoration", "line-through");
        focussed = false;
    });
    $(window).focus(function () {
        $(".focuslimit").css("text-decoration", "none");
        focussed = true;
    });

    function updateEnabled() {
        return focussed;
    }
    exports.updateEnabled = updateEnabled;
})(window);
