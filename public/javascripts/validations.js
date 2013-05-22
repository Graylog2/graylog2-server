function validate(formcontainer) {
    errors = false;

    $(formcontainer + " .validatable").each(function() {
        switch($(this).attr("data-validate")) {
         case "defined":
             if (!validateDefined($(this))) {
                 validationFailure($(this), "must be set");
                 errors = true;
             };
        }
    });

    return !errors;
};

function validationFailure(el, msg) {
    el.popover({
        content: msg
    }).popover("show");
}

// Validators.
function validateDefined(el) {
    return el.val() != undefined && el.val().length > 0;
}