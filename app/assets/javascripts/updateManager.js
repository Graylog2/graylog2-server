(function (exports, userPreferences) {
    'use strict';

    var focussed = true;
    var updateUnfocussed = userPreferences.updateUnfocussed;

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

    function getUpdateUnfocussedMode() {
        return updateUnfocussed;
    }

    exports.assertUpdateEnabled = assertUpdateEnabled;
    exports.setUpdateUnfocussedMode = setUpdateUnfocussedMode;
    exports.getUpdateUnfocussedMode = getUpdateUnfocussedMode;
})(window, userPreferences || {});
