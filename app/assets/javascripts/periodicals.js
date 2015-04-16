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