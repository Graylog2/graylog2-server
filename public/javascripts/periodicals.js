focussed = true;

$(window).blur(function(){
    $(".focuslimit").css("text-decoration", "line-through");
    focussed = false;
});
$(window).focus(function(){
    $(".focuslimit").css("text-decoration", "none");
    focussed = true;
});


$(document).ready(function() {

    // Total event counts;
    (function updateTotalEvents() {
        if ($(".total-events").length > 0) {
            var interval = 2500;

            if(!focussed) {
                setTimeout(updateTotalEvents, interval);
                return;
            }

            $.ajax({
                url: '/a/messagecounts/total',
                success: function(data) {
                    $(".total-events").animatedIntChange(data.events, 500)
                },
                error: function() {
                    $(".total-events").text("?");
                },
                complete: function() {
                    setTimeout(updateTotalEvents, interval);
                }
            });
        }
    })();

    // Total throughput.
    (function updateTotalThroughput() {
        if ($(".total-throughput").length > 0) {
            var interval = 1000;

            if(!focussed) {
                setTimeout(updateTotalThroughput, interval);
                return;
            }

            $.ajax({
                url: '/a/system/throughput',
                success: function(data) {
                    $(".total-throughput").text(data.throughput);

                    if (data.nodecount > 1) {
                        $(".total-nodes").html(" across <strong>" + data.nodecount + "</strong> nodes");
                    } else {
                        $(".total-nodes").html(" on 1 node");
                    }
                },
                error: function() {
                    $(".total-throughput").html("?");
                    $(".total-nodes").html("");
                },
                complete: function() {
                    setTimeout(updateTotalThroughput, interval);
                }
            });
        }
    })();

    // Individual node throughput.
    (function updateNodeThroughput() {
        if ($(".node-throughput").length > 0) {
            var interval = 1000;

            if(!focussed) {
                setTimeout(updateNodeThroughput, interval);
                return;
            }

            $(".node-throughput").each(function(i) {
                var thisNodeT = $(this);
                $.ajax({
                    url: '/a/system/throughput/node/' + $(this).attr("data-node-id"),
                    success: function(data) {
                        thisNodeT.text(data.throughput);
                    },
                    error: function() {
                        thisNodeT.text("?");
                    },
                    complete: function() {
                        // Trigger next call of the whole function when we updated the last element.
                        if (i == $(".node-throughput").length-1) {
                            setTimeout(updateNodeThroughput, interval);
                        }
                    }
                });
            });
        };
    })();

    // IO of input.
    (function updateInputIO() {
        var interval = 1000;

        if(!focussed) {
            setTimeout(updateInputIO, interval);
            return;
        }

        $(".input-io").each(function() {
            var inputId = $(this).attr("data-input-id");
            var nodeId = $(this).attr("data-node-id");

            var io = $(this);

            $.ajax({
                url: '/a/system/inputs/' + encodeURIComponent(nodeId) + '/' + encodeURIComponent(inputId) + '/io',
                success: function(data) {
                    $(".persec .rx", io).text(data.rx);
                    $(".persec .tx", io).text(data.tx);
                    $(".total .rx", io).text(data.total_rx);
                    $(".total .tx", io).text(data.total_tx);
                }
            });

        }).promise().done(function(){ setTimeout(updateInputIO, interval); });;
    })();

    // Connection counts of input.
    (function updateInputConnections() {
        var interval = 1000;

        if(!focussed) {
            setTimeout(updateInputConnections, interval);
            return;
        }

        $(".input-connections").each(function() {
            var inputId = $(this).attr("data-input-id");
            var nodeId = $(this).attr("data-node-id");

            var connections = $(this);

            $.ajax({
                url: '/a/system/inputs/' + encodeURIComponent(nodeId) + '/' + encodeURIComponent(inputId) + '/connections',
                success: function(data) {
                    $(".total", connections).text(data.total);
                    $(".active", connections).text(data.active);
                }
            });

        }).promise().done(function(){ setTimeout(updateInputConnections, interval); });
    })();

    // Notification count badge.
    (function updateNotificationCount() {
        $.ajax({
            url: '/a/system/notifications',
            success: function(data) {
                var count = data.count;
                if (count > 0) {
                    $("#notification-badge").text(count);
                } else {
                    // Badges are collapsing when empty so we make a 0 collapse.
                    $("#notification-badge").text("");
                }
            },
            complete: function() {
                setTimeout(updateNotificationCount, 10000);
            }
        });
    })();

});