$(document).ready(function() {
    $("div.streamrule-sample-message").sampleMessageLoader({
        subcontainer: $('div.subcontainer', $('div.streamrule-sample-message')),
        selector: $('div.manual-selector', $('div.streamrule-sample-message')),
        message: $('div.sample-message-display', $('div.streamrule-sample-message')),
        spinner: $('div.spinner', $('div.streamrule-sample-message'))
    });

    $(document.body).on("keyup change", ".sr-input", function(foo) {
        value = $(this).val();
        var modalBody = $(this).closest("form#streamrule-form").find(".modal-body");

        if ($(this).attr("id") == "sr-type") {
            if (parseInt(value) == 5) {
                $("#sr-value", modalBody).hide();
                $("#sr-label-value", modalBody).hide();
                $("#sr-result-value", modalBody).hide();
            } else {
                $("#sr-value", modalBody).show();
                $("#sr-label-value", modalBody).show();
                $("#sr-result-value", modalBody).show();
            }
        }

        if (value != undefined && value != "") {
            // Selectbox options can have a custom replace string.
            s = $("option:selected", this);
            if (s != undefined && s.attr("data-reflect-string") != undefined && s.attr("data-reflect-string") != "") {
                value = s.attr("data-reflect-string");

                // Inverted?
                if ($("#sr-inverted", modalBody).is(':checked')) {
                    value = "not " + value;
                }
            }
        } else {
            value = $(this).attr("placeholder");
        }

        $($(this).attr("data-reflect"), modalBody).text(value);
    });

    $(".streamrules-list").on("click", "li a.remove-streamrule", function(event) {
        var result = confirm("Really delete stream rule?");
        var streamId = $(this).attr('data-stream-id');
        if (result) {
            var elem = $(this).parent();
            var url = event.currentTarget.attributes["data-removeUrl"].value; // url already prefixed in template
            $.post(url, {}, function() {
                var parent_list = $(elem).closest("ul");
                elem.remove();

                var rules_count = $("li", parent_list).size();

                if (rules_count == 1) {
                    $("#stream-rules-placeholder", parent_list).show();
                }

                $(".stream-rule-count[data-stream-id="+streamId+"]").text(rules_count - 1);

                testStreamRulesAndColorize(streamId);
            });
        }

        return false;
    });

    // Stream rules inverter.
    $("form#streamrule-form").on("click", "#sr-inverted", function() {
        var modalBody = $(this).closest("form#streamrule-form").find(".modal-body");
        var old_val = $("#sr-result-category", modalBody).html();

        if ($(this).is(":checked")) {
            // Add the not.
            new_val = "not " + old_val;
        } else {
            // Remove the not.
            if (old_val.substr(0,3) == "not") {
                new_val = old_val.substr(3);
            } else {
                new_val = old_val;
            }
        }
        $("#sr-result-category", modalBody).text(new_val);
    })

    $(document.body).on("click", "button.streamrule-form-submit", function(e) {
        var form = $(this).closest("form#streamrule-form");
        var streamId = form.attr("data-stream-id");
        var streamRuleId = form.attr("data-streamrule-id");
        var modalBody = form.find(".modal-body");
        var dialog = $(this).closest("div.modal");

        rule = {
            field: $("#sr-field", modalBody).val(),
            type: parseInt($("#sr-type", modalBody).val()),
            value: $("#sr-value", modalBody).val(),
            inverted: $("#sr-inverted", modalBody).is(":checked")
        }

        var url, callback;
        var container = $(this).closest("div.streamrules-list-container");

        if (streamId != undefined) {
            url = '/streams/' + streamId + '/rules';
            callback = function(data) {
                var parent_list = $("ul.streamrules-list", $(".streamrules-list-container[data-stream-id="+streamId+"]"));
                var rules_count = $("li", parent_list).size();

                $(".stream-rule-count[data-stream-id="+streamId+"]").text(rules_count);

                container.find("ul.streamrules-list").append(data);
                container.find("li#stream-rules-placeholder").hide();
            }
        }

        if (streamRuleId != undefined) {
            streamId = form.attr("data-parent-stream-id");
            url = '/streams/' + streamId + '/rules/' + streamRuleId;
            callback = function(data) {
                container.find("ul").find("li[data-streamrule-id=" + streamRuleId + "]").replaceWith(data);
            }
        }

        $.ajax({
            url: appPrefixed(url),
            type: "POST",
            data: rule,
            dataType: "html",

            success: function(data) {
                dialog.modal("hide");
                callback(data);

                testStreamRulesAndColorize(streamId);
            }
        });

        e.preventDefault();
    });

    $(".show-stream-rule").on("click", function(e) {
        var streamId = $(this).attr("data-stream-id");
        var form = $('form#streamrule-form[data-stream-id="' + streamId + '"]');
        $("input[type=text]", form).val("");
        form.find("div.modal").modal();
        e.preventDefault();
    });

    $(".streamrules-list").on("click", "li a.edit-streamrule", function(e) {
        var streamRuleId = $(this).attr("data-streamrule-id");
        var form = $('form#streamrule-form[data-streamrule-id="' + streamRuleId + '"]');
        form.find(".sr-input").change();
        form.find("div.modal").modal();
        e.preventDefault();
    });

    // Stream match Testing functions
    $(".test-stream-rules").on("click", function(e) {
        var streamId = $(this).attr("data-stream-id");

        testStreamRulesAndColorize(streamId);

        e.preventDefault();
    });

    $("div.sample-message-display").bind("sampleMessageChanged", function(e, data) {
        var streamId = $("form#streamrule-form[data-stream-id]").attr("data-stream-id");

        testStreamRulesAndColorize(streamId, data);
    });

    function testStreamRulesAndColorize(streamId, message) {
        if (message == undefined) {
            message = jQuery.data(document.body, "message");
        }
        var container = $(".streamrules-list-container").find("div.alert");

        testStreamRules(message, streamId,
            function(result) {
                // All matched.
                container.switchClass("alert-info alert-error", "alert-success");
                $("li", container).addClass("alert-success");
                var matchStatus = $("i.match-status");
                matchStatus.show();
                matchStatus.addClass("icon icon-ok");
            },
            function (result) {
                // Not all matched.
                container.switchClass("alert-info alert-success", "alert-error");
                colorizeRuleResults(result.rules, $(".streamrules-list")[0]);
            });
    }

    function testStreamRules(message, streamId, match, nomatch, error) {
        if (message == undefined) {
            return;
        }
        var data = { "message" : message.fields };

        $.ajax({
            url: appPrefixed('/a/streams/' + streamId + '/testMatch'),
            type: "POST",
            data: JSON.stringify(data),
            contentType: "application/json",
            success: function(data) {
                if (data.matches) {
                    if (match != undefined)
                      match(data);
                } else {
                    if (nomatch != undefined)
                      nomatch(data);
                }
            }
        });
    }

    function colorizeRuleResults(rules, list) {
        $("li", list).each(function() {
            var rule = $(this);

            var streamruleId = rule.attr("data-streamrule-id");
            if (streamruleId != undefined) {
                var matchStatus = $("i.match-status", rule);
                matchStatus.show();
                matchStatus.addClass("icon");

                var match = rules[streamruleId];
                if (match != undefined) {
                    if (match) {
                        matchStatus.switchClass("icon-warning-sign", "icon-ok");
                        rule.switchClass("alert-danger alert-info", "alert-success");
                    } else {
                        matchStatus.switchClass("icon-ok", "icon-warning-sign");
                        rule.switchClass("alert-success alert-info", "alert-danger");
                    }
                }
            }
        });
    }
});
