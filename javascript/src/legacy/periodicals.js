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