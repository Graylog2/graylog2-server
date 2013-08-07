$(document).ready(function() {

    // Load random message.
    $(".xtrc-load-recent").on("click", function() {
        var container = $(this).parent().parent();
        var subcontainer = $(".subcontainer", container);

        showSpinner(subcontainer);

        $.ajax({
            url: '/a/system/inputs/' + $(this).attr("data-node-id") + '/' + $(this).attr("data-input-id") + '/recent_message',
            success: function(data) {
                showMessage($(".xtrc-message-fields", container), data.fields, data.id);
            },
            error: function() {
                showError("There was no message received by this input in the last 24 hours. Try selecting a message manually.");
                showManualMessageSelector(subcontainer);
            },
            complete: function() {
                hideSpinner(subcontainer);
            }
        });
    });

    $(".xtrc-load-manual").on("click", function() {
        var container = $(this).parent().parent();
        var subcontainer = $(".subcontainer", container);

        subcontainer.hide();

        showManualMessageSelector(subcontainer);
    });

    function showManualMessageSelector(subcontainer, container) {
        var manualSelector = $(".manual-selector", container);
        manualSelector.show();

        $(".manual-selector-form").unbind('submit').on("submit", function(event) {
            event.preventDefault();

            showSpinner(subcontainer);
            manualSelector.hide();

            var index = $("input[name=message_id]", $(this)).val();
            var messageId = $("input[name=index]", $(this)).val();

            $.ajax({
                url: '/a/messages/' + index + '/' + messageId,
                success: function(data) {
                    showMessage($(".xtrc-message-fields", container), data.fields, data.id);
                },
                error: function() {
                    showError("Could not load message. Make sure that ID and index are correct.");
                    showManualMessageSelector(subcontainer);
                },
                complete: function() {
                    hideSpinner(subcontainer);
                }
            });
        })
    }

    function showSpinner(subcontainer) {
        var spinner = "<h2><i class='icon-refresh icon-spin'></i> &nbsp;Loading message</h2>";
        subcontainer.html(spinner);
        subcontainer.show();
    }

    function hideSpinner(subcontainer) {
        subcontainer.hide();
    }

    function showMessage(dl, msg, msgId) {
        var msgContainer = dl.parent().parent();
        msgContainer.show();

        $("h2 span", msgContainer).html(msgId);

        for(var f in msg) {
            var field = f;
            var value = msg[f];
            dl.append("<dt data-field='" + field + "' data-value='" + value + "'>" + field + "</dt><dd>" + value + "</dd>");
        }

        // Bind links to next step.
        $("dt", msgContainer).bind("click", function() {
            var field = $(this).attr("data-field");
            var value = $(this).attr("data-value");

            showExtractorWizard(field, value);
            $(".xtrc-select-message").remove();

            var wizard = $(".xtrc-wizard");
            $(".xtrc-wizard-field", wizard).html(field)
            $(".xtrc-wizard-example", wizard).html(value);

            $("input[name=field]", wizard).val(field)
            $("input[name=example]", wizard).val(value);
            wizard.show();
        });
    }

    function showExtractorWizard(field, value) {
        console.log(field + ": " + value);
    }

});