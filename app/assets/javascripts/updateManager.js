(function (exports, userPreferences) {
    'use strict';

    var recheckInterval = 1000;
    var focussed = true;
    var updateUnfocussed = userPreferences.updateUnfocussed;
    var expensiveUpdatesEnabled = !userPreferences.disableExpensiveUpdates;

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

    function assertExpensiveUpdateEnabled(callback) {
        var enabled = focussed && expensiveUpdatesEnabled;

        if (!enabled) {
            setTimeout(callback, recheckInterval);
        }
        return enabled;
    }

    exports.assertUpdateEnabled = assertUpdateEnabled;
    exports.setUpdateUnfocussedMode = setUpdateUnfocussedMode;
    exports.getUpdateUnfocussedMode = getUpdateUnfocussedMode;
    exports.assertExpensiveUpdateEnabled = assertExpensiveUpdateEnabled;
})(window, userPreferences || {});
