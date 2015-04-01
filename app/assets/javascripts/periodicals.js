$(document).ready(function() {
    // Total event counts;
    (function updateTotalEvents() {
        if ($(".total-events").length > 0) {
            var interval = 2500;
            if (!assertExpensiveUpdateEnabled()) {
                $(".expensive-update").hide();
                return;
            }
            if (!assertUpdateEnabled(updateTotalEvents)) return;

            $.ajax({
                url: appPrefixed('/a/messagecounts/total'),
                headers: { "X-Graylog2-No-Session-Extension" : "true"},
                success: function(data) {
                    $(".total-events").intChange(numeral(data.events).format("0,0"))
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
                        thisNodeT.text(numeral(data.throughput).format("0,0"));
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
        }
    })();

    // Node heap usage.
    (function updateNodeHeapUsage() {
        if ($(".graylog-node-heap").length > 0) {
            var interval = 1000;
            if (!assertUpdateEnabled(updateNodeHeapUsage)) return;

            $(".graylog-node-heap").each(function(i) {
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

                        $(".heap-used", thisHeap).text(numeral(data.used).format("0,0"));
                        $(".heap-total", thisHeap).text(numeral(data.total).format("0,0"));
                        $(".heap-max", thisHeap).text(numeral(data.max).format("0,0"));
                    },
                    complete: function() {
                        // Trigger next call of the whole function when we updated the last element.
                        if (i == $(".graylog-node-heap").length-1) {
                            setTimeout(updateNodeHeapUsage, interval);
                        }
                    }
                });
            });
        };
    })();

    // Node journal usage.
    (function updateNodeJournalUsage() {
        var nodeJournalInformation = $(".node-journal-information");
        if (nodeJournalInformation.length > 0) {
            var interval = 1000;
            if (!assertUpdateEnabled(updateNodeJournalUsage)) return;

            nodeJournalInformation.each(function(i) {
                var url = "/a/system/node/" + $(this).attr("data-node-id") + "/journal";

                var thisJournal = $(this);
                $.ajax({
                    url: appPrefixed(url),
                    headers: { "X-Graylog2-No-Session-Extension" : "true"},
                    success: function(data) {
                        var journal = $(".journal-uncommitted", thisJournal);

                        if (journal.length > 0) {
                            journal.text(numeral(data.uncommitted_entries).format("0,0"));
                        }
                    },
                    complete: function() {
                        // Trigger next call of the whole function when we updated the last element.
                        if (i == nodeJournalInformation.length-1) {
                            setTimeout(updateNodeJournalUsage, interval);
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
                        logs.intChange(numeral(data.total).format("0,0"));
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
                            $(".loglevel-metric-total", list).intChange(numeral(metrics.total).format("0,0"));
                            $(".loglevel-metric-mean", list).intChange(numeral(metrics.mean_rate).format("0,0.[00]"));
                            $(".loglevel-metric-1min", list).intChange(numeral(metrics.one_min_rate).format("0,0.[00]"));
                        }
                    }
                });

            }).promise().done(function(){ setTimeout(updateLogLevelMetrics, interval); });
        }
    })();

});