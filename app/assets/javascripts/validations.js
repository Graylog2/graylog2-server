function validate(formcontainer) {
    errors = false;
    $(formcontainer + " .validatable").each(function() {
        // Do not check disabled form fields.
        if(!$(this).is(':disabled')) {
            var validatorTypes = $(this).attr("data-validate").split(" ");
            for (var i = 0; i < validatorTypes.length; i++) {
                dispatchRuleValidation($(this), validatorTypes[i]);
            }
        }
    });

    return !errors;
}

function dispatchRuleValidation(ref, validatorType) {
    switch(validatorType) {
        case "defined":
            if (!validateDefined(ref)) {
                validationFailure(ref, "must be set");
                errors = true;
            }
            break;
        case "port_number":
            if (!validatePortNumber(ref)) {
                validationFailure(ref, "must be a valid port number");
                errors = true;
            }
            break;
        case "number":
            if (!validateNumber(ref)) {
                validationFailure(ref, "must be a number");
                errors = true;
            }
            break;
        case "positive_number":
            if (!validatePositiveNumber(ref)) {
                validationFailure(ref, "must be a positive number");
                errors = true;
            }
            break;
        case "negative_number":
            if (!validateNegativeNumber(ref)) {
                validationFailure(ref, "must be a negative number");
                errors = true;
            }
            break;
        case "not_negative_number":
            if (!validateNotNegativeNumber(ref)) {
                validationFailure(ref, "must be a not negative number");
                errors = true;
            }
            break;
        case "alphanum_underscore":
            if (!validateAlphanumericUnderscores(ref)) {
                validationFailure(ref, "must only contain alphanum chars or underscores");
                errors = true;
            }
            break;
        case "datetime_format":
            if (!validateDatetimeFormat(ref)) {
                validationFailure(ref, "is not in a valid format");
                errors = true;
            }
            break;
        case "timerange":
            if (!validateAbsoluteTimerange(ref)) {
                validationFailure(ref, "cannot be earlier than 'From'");
                errors = true;
            }
            break;
    }
}

function validationFailure(el, msg) {
    el.popover({
        container: 'body',
        content: msg,
        placement: 'auto'
    }).popover("show");
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