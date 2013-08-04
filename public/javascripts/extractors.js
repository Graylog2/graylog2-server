$(document).ready(function() {

    // Load random message.
    $(".xtrc-load-recent").on("click", function() {
        var container = $(this).parent().parent();
        var subcontainer = $(".subcontainer", container);

        showSpinner(subcontainer);

        $.ajax({
            url: '/a/system/inputs/' + $(this).attr("data-node-id") + '/' + $(this).attr("data-input-id") + '/recent_message',
            success: function(data) {
                showMessage($(".xtrc-message-fields", container), data.message);
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

            var messageId = $("input[name=message_id]", $(this)).val();
            var index = $("input[name=index]", $(this)).val();

            $.ajax({
                url: '/a/messages/' + index + '/' + messageId,
                success: function(data) {
                    showMessage($(".xtrc-message-fields", container), data.message);
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

    function showMessage(dl, msg) {
        dl.show();
        for(var f in msg) {
            dl.append("<dt>" + f + "</dt><dd>" + msg[f] + "</dd>");
        }
    }

});