$(document).ready(function() {
    $(".test-stream-rules").on("click", function() {
        var message = jQuery.data(document.body, "message");
        var streamId = $(this).attr("data-stream-id");
        var data = { "message" : message.fields };

        testStreamRules(message, streamId,
            function(result) {
                $("#streamrules-list-container")[0].classList.remove("alert-error");
                $("#streamrules-list-container")[0].classList.remove("alert-info");
                $("#streamrules-list-container")[0].classList.add("alert-success");
            },
            function (result) {
                $("#streamrules-list-container")[0].classList.remove("alert-success");
                $("#streamrules-list-container")[0].classList.remove("alert-info");
                $("#streamrules-list-container")[0].classList.add("alert-error");
                colorizeRuleResults(result.rules, $(".streamrules-list")[0]);
            });
    });

    function testStreamRules(message, streamId, match, nomatch, error) {
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
                    rule.className += " alert-success";
                } else {
                    rule.className += " alert-danger";
                }
            }
        }
    }
});
