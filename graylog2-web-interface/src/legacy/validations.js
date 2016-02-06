require('!script!../../public/javascripts/jquery-2.1.1.min.js');
require('!script!../../public/javascripts/bootstrap.min.js');

$(document).ready(function () {
    "use strict";

    var $createUsernameField = $("form#create-user-form #username");
    if ($createUsernameField.length > 0) {
        var domElement = $createUsernameField[0];
        delayedAjaxCallOnKeyup(domElement, function () {
            var username = $createUsernameField.val();
            $.ajax({
                url: appPrefixed("/a/system/users/" + encodeURIComponent(username)),
                type: "GET",
                cache: false,
                global: false,
                statusCode: {
                    204: function () {
                        $createUsernameField.setCustomValidity('The entered user name is already taken.');
                    },
                    404: function () {
                        $createUsernameField.setCustomValidity('');
                    }
                }
            });
        }, 150);
    }

    var $passwordField = $("form #password");
    if ($passwordField.length > 0) {
        $passwordField.on('keyup', function () {
            var password = $passwordField.val();
            if (password.length < 6) {
                $passwordField.setCustomValidity("Password is too short!");
            } else {
                $passwordField.setCustomValidity('');
            }
        });
    }

    var $repeatPasswordField = $("form #password-repeat");
    if ($repeatPasswordField.length) {
        $repeatPasswordField.on('keyup', function () {
            var $password = $("form #password").val();
            if ($password == $repeatPasswordField.val()) {
                $repeatPasswordField.setCustomValidity('');
            } else {
                $repeatPasswordField.setCustomValidity("Passwords do not match!");
            }
        });
    }
});


export function validate(formContainer) {
    var errors = false;
    $(".validatable", formContainer).each(function () {
        // Do not check disabled form fields.
        if (!$(this).is(':disabled')) {
            var dataValidations = $(this).attr("data-validate");
            if (dataValidations !== undefined) {
                var validatorTypes = dataValidations.split(" ");
                for (var i = 0; (!errors && i < validatorTypes.length); i++) {
                    errors = dispatchRuleValidation($(this), validatorTypes[i]);
                }
            }
        }
    });

    return !errors;
}

function dispatchRuleValidation($ref, validatorType) {
    var errors = false;

    switch (validatorType) {
        case "defined":
            if (!validateDefined($ref)) {
                validationFailure($ref, "Must be set");
                errors = true;
            }
            break;
        case "port_number":
            if (!validatePortNumber($ref)) {
                validationFailure($ref, "Must be a valid port number");
                errors = true;
            }
            break;
        case "number":
            if (!validateNumber($ref)) {
                validationFailure($ref, "Must be a number");
                errors = true;
            }
            break;
        case "positive_number":
            if (!validatePositiveNumber($ref)) {
                validationFailure($ref, "Must be a positive number");
                errors = true;
            }
            break;
        case "negative_number":
            if (!validateNegativeNumber($ref)) {
                validationFailure($ref, "Must be a negative number");
                errors = true;
            }
            break;
        case "not_negative_number":
            if (!validateNotNegativeNumber($ref)) {
                validationFailure($ref, "Must be a not negative number");
                errors = true;
            }
            break;
        case "alphanum_underscore":
            if (!validateAlphanumericUnderscores($ref)) {
                validationFailure($ref, "Must only contain alphanumeric characters or underscores");
                errors = true;
            }
            break;
        case "datetime_format":
            if (!validateDatetimeFormat($ref)) {
                validationFailure($ref, "Is not in a valid format");
                errors = true;
            }
            break;
        case "timerange":
            if (!validateAbsoluteTimerange($ref)) {
                validationFailure($ref, "cannot be earlier than 'From'");
                errors = true;
            }
            break;
    }
    return errors;
}

function validationFailure($el, msg) {
    "use strict";
    $el.popover({
        content: msg,
        placement: 'bottom',
        trigger: 'manual'
    });
    $el.on('shown.bs.popover', function () {
        window.setTimeout(function () {
            $el.popover('destroy');
        }, 3000);
    });
    $el.popover('show');
}

// Validators.
function validateDefined(el) {
    return el.val() != undefined && el.val().length > 0;
}

function validatePortNumber(el) {
    var i = parseInt(el.val());
    return i > 0 && i < 65535;
}

function validatePositiveNumber(el) {
    return parseInt(el.val()) > 0;
}

function validateNegativeNumber(el) {
    return parseInt(el.val()) < 0;
}

function validateAlphanumericUnderscores(el) {
    return el.val().match("^\\w*$");
}

function validateNotNegativeNumber(el) {
    return (validatePositiveNumber(el) || parseInt(el.val()) == 0);
}

function validateNumber(el) {
    return isNumber(el.val());
}

function validateDatetimeFormat(el) {
    var dateString = $(el).val();
    return momentHelper.parseFromString(dateString).isValid();
}

function validateAbsoluteTimerange(el) {
    var parent = $(el).parent().parent();
    var fromStr = $("input[name='from']", parent).val();
    var toStr = $("input[name='to']", parent).val();
    var from = momentHelper.parseFromString(fromStr);
    var to = momentHelper.parseFromString(toStr);

    return (from <= to)
}