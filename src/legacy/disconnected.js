$(document).ready(function () {

    // make the config node box clickable
    $(".discovered-node-link").on("click", function () {
        var nodeId = $(this).attr("data-discovered-node");
        var nodeElem = $("div[data-node-id=\"" + nodeId + "\"]");
        nodeElem.effect("pulsate");
    });

    $(".via-node-headline").on("click", function () {
        var address = $(this).attr("data-discovered-via");
        var nodeElem = $("div[data-config-node-address=\"" + address + "\"]");
        nodeElem.effect("pulsate");
    });

    // Notification count badge.
    (function checkServerAvailability() {
        $.ajax({
            url: gl2AppPathPrefix + '/a/connection/available',
            success: function(data) {
                var connected = data.connected;
                var count = data.connected_nodes_count;
                var total = data.total_nodes_count;

                if (total == 0) {
                    $("#total-count-zero").removeClass("hidden");
                    $("#total-count-nonzero").addClass("hidden");
                } else {
                    $("#total-count-zero").addClass("hidden");
                    $("#total-count-nonzero").removeClass("hidden");
                }
                $("#connected-count").html(count);
                $("#total-count").html(total);
                $(".footer").removeClass("hidden");
                if (connected) {
                    $("#username").prop("disabled", false);
                    $("#password").prop("disabled", false);
                    $("#checkconnection").addClass("hidden");
                    $("#signin").removeClass("hidden");
                } else {
                    $("#username").prop("disabled", true);
                    $("#password").prop("disabled", true);
                    $("#checkconnection").removeClass("hidden");
                    $("#signin").addClass("hidden");
                }
            },
            complete: function() {
                setTimeout(checkServerAvailability, 1000);
            }
        });
    })();

    $('input, textarea').placeholder();

});

