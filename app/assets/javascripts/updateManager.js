(function (exports) {
    'use strict';

    var focussed = true;
    var updateUnfocussed = false;

    $(window).blur(function () {
        setFocus(false);
    });
    $(window).focus(function () {
        setFocus(true);
    });

    function setFocus(focus) {
        focussed = updateUnfocussed || focus;
        updateFocusLimitedElements();
    }

    function updateFocusLimitedElements() {
        var style = focussed ? "none" : "line-through";
        $(".focuslimit").css("text-decoration", style);
    }

    function assertUpdateEnabled(callback) {
        var recheckInterval = 1000;

        if (!focussed) {
            setTimeout(callback, recheckInterval);
        }
        return focussed;
    }

    function setUpdateUnfocussedMode(enabled) {
        updateUnfocussed = enabled;
        setFocus(focussed);
    }

    exports.assertUpdateEnabled = assertUpdateEnabled;
    exports.setUpdateUnfocussedMode = setUpdateUnfocussedMode;
})(window);
