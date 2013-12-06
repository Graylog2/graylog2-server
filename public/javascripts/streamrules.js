$(document).ready(function() {
    $(document.body).on("keyup change", ".sr-input", function(foo) {
        console.log(foo);

        console.log("keyup change");
        value = $(this).val();
        var modalBody = $(this).closest("form#streamrule-form").find(".modal-body");

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

        $($(this).attr("data-reflect"), modalBody).html(value);
    });

    $(".streamrules-list").on("click", "li a.remove-streamrule", function(event) {
        var result = confirm("Really delete stream rule?");
        var streamId = $(this).attr('data-stream-id');
        if (result) {
            var elem = $(this).parent();
            var url = event.currentTarget.attributes["data-removeUrl"].value;
            $.post(url, {}, function() {
                var parent_list = $(elem).closest("ul");
                elem.remove();

                if ($("li", parent_list).size() == 1) {
                    $("#stream-rules-placeholder", parent_list).show();
                }

                testStreamRulesAndColorize(streamId);
            });
        }

        return false;
    });

    // Stream rules inverter.
    $(".streamrules-list").on("click", "#sr-inverted", function() {
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
        $("#sr-result-category", modalBody).html(new_val);
    })

    // Add stream rule to stream rule list when saved.
    var rule_count;
    function addRuleToNewStream(rule) {
        if (rule_count == undefined) {
            rule_count = 0;
        } else {
            rule_count++;
        }
        if (!validate("#sr")) {
            return false;
        }

        $("#stream-rules-placeholder").hide();

        // Add hidden field that is transmitted in form add visible entry.
        field = "<input type='hidden' name='rules["+rule_count+"].field' value='" + rule.field + "' />\n" +
            "<input type='hidden' name='rules["+rule_count+"].type' value='" + rule.type + "' />\n" +
            "<input type='hidden' name='rules["+rule_count+"].value' value='" + rule.value + "' />\n" +
            "<input type='hidden' name='rules["+rule_count+"].inverted' value='" + rule.inverted + "' />\n"

        remover = "<a href='#' class='sr-remove'><i class='icon-remove'></i></a>";
        $("#stream-rules").append("<li id='rule'>" + field + $("#sr-result").html().replace(/<(?:.|\n)*?>/gm, '') + " " + remover + "</li>");

        // Remove stream rule binding.
        $(".sr-remove").on("click", function() {
            var parent_list = $(this).parents("ul");
            $(this).parent().remove();
            renumber_rules(parent_list);
            return false;
        });

        var renumber_rules = function($rules) {
            $('li#rule', $rules).each(function($index) {
                $('input', $(this)).each (function() {
                    var new_name = $(this).attr('name').replace(/rules\[\d+\]/g, 'rules['+$index+']');
                    $(this).attr('name', new_name);
                });
            });
        }
    }

    $(document.body).on("click", "button.streamrule-form-submit", function() {
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

        if (streamId != undefined || streamRuleId != undefined) {
            var url, callback;
            var container = $(this).closest("div#streamrules-list-container");

            if (streamId != undefined) {
                url = '/streams/' + streamId + '/rules';
                callback = function(data) {
                    container.find("ul").append(data);
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
                url: url,
                type: "POST",
                data: rule,
                dataType: "html",

                success: function(data) {
                    dialog.modal("hide");
                    callback(data);

                    testStreamRulesAndColorize(streamId);
                }
            });
        } else {
            addRuleToNewStream(rule);
            dialog.modal("hide");
        }

        return false;
    });

    $(".show-stream-rule").on("click", function() {
        var streamId = $(this).attr("data-stream-id");
        var form = $('form#streamrule-form[data-stream-id="' + streamId + '"]');
        form.find("div.modal").modal();
        return false;
    });

    $(".streamrules-list").on("click", "li a.edit-streamrule", function(event) {
        var streamRuleId = $(this).attr("data-streamrule-id");
        var form = $('form#streamrule-form[data-streamrule-id="' + streamRuleId + '"]');
        form.find(".sr-input").change();
        form.find("div.modal").modal();
        return false;
    });

    // Stream match Testing functions
    $(".test-stream-rules").on("click", function() {
        var streamId = $(this).attr("data-stream-id");

        testStreamRulesAndColorize(streamId);

        return false;
    });

    $("div.xtrc-message").bind("sampleMessageChanged", function() {
        var streamId = $("form#streamrule-form[data-stream-id]").attr("data-stream-id");

        testStreamRulesAndColorize(streamId);
    });

    function testStreamRulesAndColorize(streamId) {
        var message = jQuery.data(document.body, "message");

        if (message == undefined) {
            return;
        }
        var container = $("#streamrules-list-container").find("div.alert");

        testStreamRules(message, streamId,
            function(result) {
                container[0].classList.remove("alert-error");
                container[0].classList.remove("alert-info");
                container[0].classList.add("alert-success");
            },
            function (result) {
                container[0].classList.remove("alert-success");
                container[0].classList.remove("alert-info");
                container[0].classList.add("alert-error");
                colorizeRuleResults(result.rules, $(".streamrules-list")[0]);
            });
    }

    function testStreamRules(message, streamId, match, nomatch, error) {
        if (message == undefined) {
            return;
        }
        var data = { "message" : message.fields };

        $.ajax({
            url: '/a/streams/' + streamId + '/testMatch',
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
        var ruleslist = $("li", list);
        for (var i=0; i < ruleslist.size(); i++) {
            var rule = ruleslist[i];
            var streamruleId = rule.getAttribute("data-streamrule-id");
            if (streamruleId == undefined) continue;
            var match = rules[streamruleId];
            if (match != undefined) {
                if (match) {
                    rule.classList.add("alert-success")
                    rule.classList.remove("alert-danger")
                } else {
                    rule.classList.add("alert-danger")
                    rule.classList.remove("alert-success")
                }
            }
        }
    }
});
