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
};

function dispatchRuleValidation(ref, validatorType) {
    switch(validatorType) {
        case "defined":
            if (!validateDefined(ref)) {
                validationFailure(ref, "must be set");
                errors = true;
            };
            break;
        case "port_number":
            if (!validatePortNumber(ref)) {
                validationFailure(ref, "must be a valid port number");
                errors = true;
            };
            break;
        case "positive_number":
            if (!validatePositiveNumber(ref)) {
                validationFailure(ref, "must be a positive number");
                errors = true;
            };
            break;
        case "negative_number":
            if (!validateNegativeNumber(ref)) {
                validationFailure(ref, "must be a negative number");
                errors = true;
            };
            break;
    }
}

function validationFailure(el, msg) {
    el.popover({
        content: msg
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