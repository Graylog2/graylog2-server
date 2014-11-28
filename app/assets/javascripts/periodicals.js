$(document).ready(function() {
    // Total event counts;
    (function updateTotalEvents() {
        if ($(".total-events").length > 0) {
            var interval = 2500;
            if (!assertExpensiveUpdateEnabled(updateTotalEvents)) {
                $(".expensive-update").hide();
                return;
            }
            if (!assertUpdateEnabled(updateTotalEvents)) return;

            $.ajax({
                url: appPrefixed('/a/messagecounts/total'),
                headers: { "X-Graylog2-No-Session-Extension" : "true"},
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
            if (!assertUpdateEnabled(updateTotalThroughput)) return;

            $.ajax({
                url: appPrefixed('/a/system/throughput'),
                headers: { "X-Graylog2-No-Session-Extension" : "true"},
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
            if (!assertUpdateEnabled(updateNodeThroughput)) return;

            $(".node-throughput").each(function(i) {
                var nodeType = $(this).attr("data-node-type");
                var url;
                if (!!nodeType && $(this).attr("data-node-type") == "radio") {
                    url = "/a/system/throughput/radio/" + $(this).attr("data-radio-id");
                } else if (!!nodeType && $(this).attr("data-node-type") == "stream") {
                    url = "/a/system/throughput/stream/" + $(this).attr("data-stream-id");
                } else {
                    url = "/a/system/throughput/node/" + $(this).attr("data-node-id");
                }

                var thisNodeT = $(this);
                $.ajax({
                    url: appPrefixed(url),
                    headers: { "X-Graylog2-No-Session-Extension" : "true"},
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

    // Node heap usage.
    (function updateNodeHeapUsage() {
        if ($(".node-heap-usage").length > 0) {
            var interval = 1000;
            if (!assertUpdateEnabled(updateNodeHeapUsage)) return;

            $(".node-heap-usage").each(function(i) {
                var nodeType = $(this).attr("data-node-type");
                var url;
                if (!!nodeType && $(this).attr("data-node-type") == "radio") {
                    url = "/a/system/radio/" + $(this).attr("data-radio-id") + "/heap"
                } else {
                    url = "/a/system/node/" + $(this).attr("data-node-id") + "/heap"
                }

                var thisHeap = $(this);
                $.ajax({
                    url: appPrefixed(url),
                    headers: { "X-Graylog2-No-Session-Extension" : "true"},
                    success: function(data) {
                        var total_percentage = data.total_percentage-data.used_percentage;
                        $(".progress .heap-used-percent", thisHeap).css("width", data.used_percentage + "%");
                        $(".progress .heap-total-percent", thisHeap).css("width", total_percentage + "%");

                        $(".heap-used", thisHeap).text(data.used);
                        $(".heap-total", thisHeap).text(data.total);
                        $(".heap-max", thisHeap).text(data.max);

                        $(".input-master-cache", thisHeap).text(data.input_master_cache);
                        $(".output-master-cache", thisHeap).text(data.output_master_cache);
                    },
                    complete: function() {
                        // Trigger next call of the whole function when we updated the last element.
                        if (i == $(".node-heap-usage").length-1) {
                            setTimeout(updateNodeHeapUsage, interval);
                        }
                    }
                });
            });
        };
    })();

    // IO of input.
    (function updateInputIO() {
        var interval = 1000;
        if (!assertUpdateEnabled(updateInputIO)) return;

        $(".global-input-io").each(function() {
            var inputId = $(this).attr("data-input-id");
            var io = $(this);
            $.ajax({
                url: appPrefixed('/a/system/inputs/global/' + encodeURIComponent(inputId) + '/io'),
                headers: { "X-Graylog2-No-Session-Extension" : "true"},
                success: function(data) {
                    $(".global-persec .rx", io).text(data.rx);
                    $(".global-persec .tx", io).text(data.tx);
                    $(".global-total .rx", io).text(data.total_rx);
                    $(".global-total .tx", io).text(data.total_tx);
                }
            });
        });

        $(".input-io").each(function() {
            var inputId = $(this).attr("data-input-id");
            var nodeId = $(this).attr("data-node-id");

            var io = $(this);

            $.ajax({
                url: appPrefixed('/a/system/inputs/' + encodeURIComponent(nodeId) + '/' + encodeURIComponent(inputId) + '/io'),
                headers: { "X-Graylog2-No-Session-Extension" : "true"},
                success: function(data) {
                    $(".persec .rx", io).text(data.rx);
                    $(".persec .tx", io).text(data.tx);
                    $(".total .rx", io).text(data.total_rx);
                    $(".total .tx", io).text(data.total_tx);
                    $(".persec .rx", io).data("persec-rx-value", data.rx);
                    $(".persec .tx", io).data("persec-tx-value", data.tx);
                    $(".total .rx", io).data("total-rx-value", data.total_rx);
                    $(".total .tx", io).data("total-tx-value", data.total_tx);
                }
            });
        }).promise().done(function(){ setTimeout(updateInputIO, interval); });;
    })();


    function printShortSize(x) {
        var units = {
            0:"B",
            1:"kiB",
            2:"MiB",
            3:"GiB",
            4:"TiB",
            5:"PiB"
        };

        if (x == 0)
            return "0B";

        var nearestUnit = Math.floor(Math.log(x)/Math.log(1024));
        var result = (x/Math.pow(1024, nearestUnit)).toFixed(1);
        if (result.indexOf(".0") > -1)
            result = result.replace(".0", "");

        return (result + units[nearestUnit]).replace(".", ",");
    }

    function parseShortSize(x) {
        var units = {
            0:"B",
            1:"kiB",
            2:"MiB",
            3:"GiB",
            4:"TiB",
            5:"PiB"
        };

        x = x.replace(",", ".");

        for (var i = 5; i >= 0; i--) {
            if (x.indexOf(units[i]) > 0) {
                var result = x.replace(units[i], "");
                return result*Math.pow(1024, i);
            }
        }
    }

    // IO of global input.
    (function updateGlobalInputIO() {
        var interval = 1000;
        if (!assertUpdateEnabled(updateGlobalInputIO)) return;

        var globalInputs = $(".global-input-connections").map(
            function(x) {
                return $(this).attr("data-input-id")
            }
        );

        globalInputs.each(function(x,y) {
            var count = $(".global-input-io-details[data-input-id="+y+"] .input-io .persec .rx").map(
                function(a,b) {
                    return parseShortSize($(b).text());
                }
            ).toArray().reduce(function(a,b){return a+b;});
            $(".global-input-io[data-input-id="+y+"] .global-persec .rx").text(printShortSize(count));

            var count = $(".global-input-io-details[data-input-id="+y+"] .input-io .persec .tx").map(
                function(a,b) {
                    return parseShortSize($(b).text());
                }
            ).toArray().reduce(function(a,b){return a+b;});
            $(".global-input-io[data-input-id="+y+"] .global-persec .tx").text(printShortSize(count));

            var count = $(".global-input-io-details[data-input-id="+y+"] .input-io .total .rx").map(
                function(a,b) {
                    return parseShortSize($(b).text());
                }
            ).toArray().reduce(function(a,b){return a+b;});
            $(".global-input-io[data-input-id="+y+"] .global-total .rx").text(printShortSize(count));

            var count = $(".global-input-io-details[data-input-id="+y+"] .input-io .total .tx").map(
                function(a,b) {
                    return parseShortSize($(b).text());
                }
            ).toArray().reduce(function(a,b){return a+b;});
            $(".global-input-io[data-input-id="+y+"] .global-total .tx").text(printShortSize(count));
        }).promise().done(function(){ setTimeout(updateGlobalInputIO, interval); });;
    })();


    // Connection counts of input.
    (function updateInputConnections() {
        var interval = 1000;

        if (!assertUpdateEnabled(updateInputConnections)) return;

        $(".input-connections").each(function() {
            var inputId = $(this).attr("data-input-id");
            var nodeId = $(this).attr("data-node-id");

            var connections = $(this);

            $.ajax({
                url: appPrefixed('/a/system/inputs/' + encodeURIComponent(nodeId) + '/' + encodeURIComponent(inputId) + '/connections'),
                headers: { "X-Graylog2-No-Session-Extension" : "true"},
                success: function(data) {
                    $(".total", connections).text(data.total);
                    $(".active", connections).text(data.active);
                }
            });

        }).promise().done(function(){ setTimeout(updateInputConnections, interval); });
    })();

    // Connection counts of global input.
    (function updateGlobalInputConnections() {
        var interval = 1000;
        if (!assertUpdateEnabled(updateGlobalInputConnections)) return;

        var globalInputs = $(".global-input-connections").map(
            function(x) {
                return $(this).attr("data-input-id")
            }
        );

        globalInputs.each(function(x,y) {
            var count = $(".global-input-connection-details[data-input-id="+y+"] .input-connections .total").map(
                function(a,b) {
                    return parseInt($(b).text());
                }
            ).toArray().reduce(function(a,b){return a+b;});
            $(".global-input-connections[data-input-id="+y+"] .global-total").text(count);

            var count = $(".global-input-connection-details[data-input-id="+y+"] .input-connections .active").map(
                function(a,b) {
                    return parseInt($(b).text());
                }
            ).toArray().reduce(function(a,b){return a+b;});
            $(".global-input-connections[data-input-id="+y+"] .global-active").text(count);
        }).promise().done(function(){ setTimeout(updateGlobalInputConnections, interval); });;

    })();


    // Notification count badge.
    (function updateNotificationCount() {
        var interval = 10000;
        if (!assertUpdateEnabled(updateNotificationCount)) return;

        $.ajax({
            url: appPrefixed('/a/system/notifications'),
            headers: { "X-Graylog2-No-Session-Extension" : "true"},
            success: function(data) {
                var notificationBadgeElement = $("#notification-badge");
                var count = data.length;
                if (count > 0) {
                    notificationBadgeElement.text(count);
                    var urgent = data.filter(function(x) { return x.severity == "URGENT"});
                    if (urgent.length > 0) {
                        if (!notificationBadgeElement.data("bouncing")) {
                            var bouncer = setInterval(function() {
                                if (notificationBadgeElement.data("bouncing")) {
                                    notificationBadgeElement.effect("fade", "fast");
                                }
                            }, 750);

                            $("#notification-badge").data("bouncing", bouncer);
                        }
                    } else {
                        if (notificationBadgeElement.data("bouncing")) {
                            clearInterval(notificationBadgeElement.data("bouncing"));
                            notificationBadgeElement.data("bouncing", undefined);
                        }
                    }
                    notificationBadgeElement.show();
                } else {
                    // Badges are collapsing when empty so we make a 0 collapse.
                    notificationBadgeElement.text("");
                    notificationBadgeElement.hide();
                    if (notificationBadgeElement.data("bouncing")) {
                        clearInterval(notificationBadgeElement.data("bouncing"));
                        notificationBadgeElement.data("bouncing", undefined);
                    }
                }
            },
            complete: function() {
                setTimeout(updateNotificationCount, interval);
            }
        });
    })();

    // Total log message counts of a node;
    (function updateTotalLogs() {
        if ($(".total-logs").length > 0) {
            var interval = 1000;
            if (!assertUpdateEnabled(updateTotalLogs)) return;

            $(".total-logs").each(function() {
                var nodeId = $(this).attr("data-node-id");

                var logs = $(this);
                $.ajax({
                    url: appPrefixed('/a/system/internallogs/' + encodeURIComponent(nodeId)),
                    headers: { "X-Graylog2-No-Session-Extension" : "true"},
                    success: function(data) {
                        logs.animatedIntChange(data.total, 500);
                    }
                });

            }).promise().done(function(){ setTimeout(updateTotalLogs, interval); });
        }
    })();

    // Total log level metrics of a node;
    (function updateLogLevelMetrics() {
        if ($(".loglevel-metrics").length > 0) {
            var interval = 1000;
            if (!assertUpdateEnabled(updateLogLevelMetrics)) return;

            $(".loglevel-metrics:visible").each(function() {
                var nodeId = $(this).attr("data-node-id");

                var theseMetrics = $(this);
                $.ajax({
                    url: appPrefixed('/a/system/internallogs/' + encodeURIComponent(nodeId) + '/metrics'),
                    headers: { "X-Graylog2-No-Session-Extension" : "true"},
                    success: function(data) {
                        for (var level in data) {
                            var metrics = data[level];
                            var list = $("dl.loglevel-metrics-list[data-level=" + level + "]", theseMetrics);
                            $(".loglevel-metric-total", list).animatedIntChange(metrics.total, 500);
                            $(".loglevel-metric-mean", list).animatedIntChange(metrics.mean_rate, 500);
                            $(".loglevel-metric-1min", list).animatedIntChange(metrics.one_min_rate, 500);
                        }
                    }
                });

            }).promise().done(function(){ setTimeout(updateLogLevelMetrics, interval); });
        }
    })();

});