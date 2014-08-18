$(document).ready(function() {

    $(".analyze-field .show-quickvalues").on("click", function(e) {
        if ($(this).attr("disabled") == "disabled") {
            return;
        }

        e.preventDefault();

        // Hide and disable all others.
        $(".quickvalues").hide();
        $(".show-quickvalues").removeAttr("disabled");
        $(".quickvalues").attr("data-active", "false");

        // Mark this one as active.
        $(".quickvalues", $(this).parent()).attr("data-active", "true");

        $(this).attr("disabled", "disabled");

        showQuickValues($(this).attr("data-field"), $(this).parent(), true, calculateDirection($(this)), true);
    });

    $(".quickvalues .quickvalues-refresh").on("click", function(e) {
        e.preventDefault();

        var button = $(".analyze-field .show-quickvalues[data-field='" + $(this).attr("data-field") + "']");
        var quickvalues = $(".quickvalues[data-field='" + $(this).attr("data-field") + "']");

        showQuickValues($(this).attr("data-field"), quickvalues.parent(), true, calculateDirection(button), false);
    });

    $(".quickvalues .quickvalues-export").on("click", function(e) {
        e.preventDefault();

        // TODO
        alert("Exporting statistics is not implemented yet. (GitHub issue: #239)");
    });

    $(".quickvalues .quickvalues-close").on("click", function(e) {
        e.preventDefault();

        var quickvalues = $(".quickvalues[data-field='" + $(this).attr("data-field") + "']");
        var button = $(".analyze-field .show-quickvalues[data-field='" + $(this).attr("data-field") + "']");

        button.removeAttr("disabled");

        quickvalues.attr("data-active", "false");
        quickvalues.hide();
    });

    $(".quickvalues .quickvalues-autorefresh").on("click", function(e) {
        e.preventDefault();
        var quickvalues = $(".quickvalues[data-field='" + $(this).attr("data-field") + "']");;

        if ($(this).hasClass("active")) {
            // Disabling autorefresh.
            quickvalues.attr("data-autorefresh", "false");
            $(this).removeClass("active");
        } else {
            // Enabling autorefresh.
            quickvalues.attr("data-autorefresh", "true");
            $(this).addClass("active");

            // Load once to trigger reload cycle again.
            var button = $(".analyze-field .show-quickvalues[data-field='" + $(this).attr("data-field") + "']");
            showQuickValues($(this).attr("data-field"), quickvalues.parent(), true, calculateDirection(button), true);
        }
    });

    function showQuickValues(field, container, manualReload, direction, reload) {
        var quickvalues = $(".quickvalues", container);

        // Never update anything if this is a non-visible reload and we are not active or auto-refresh is disabled.
        // Prevents unneeded calculations when the windows is hidden and auto-refresh is enabled.
        if (reload && (quickvalues.attr("data-active") != "true" || quickvalues.attr("data-autorefresh") == "false")) {
            return;
        }

        var inlineSpin = "<i class='icon icon-spinner icon-spin'></i>";

        if (manualReload) {
            $(".terms-total", quickvalues).html(inlineSpin);
            $(".terms-missing", quickvalues).html(inlineSpin);
            $(".terms-other", quickvalues).html(inlineSpin);

            $(".terms tbody", quickvalues).empty();
            $(".terms tbody", quickvalues).append("<tr><td colspan='3'>" + inlineSpin + "</td></tr>");

            $(".terms-distribution", quickvalues).hide();
        }

        var button = $(".analyze-field .show-quickvalues[data-field='" + field + "']");
        updatePosition(button, quickvalues, direction);

        switch(direction)  {
            case "up":
                quickvalues.removeClass("quickvalues-down");
                quickvalues.addClass("quickvalues-up");
                break;
            case "down":
                quickvalues.removeClass("quickvalues-up");
                quickvalues.addClass("quickvalues-down");
                break;
        }

        quickvalues.show();

        if(reload) {
            $(".quickvalues-autorefresh", quickvalues).addClass("loading");
        }

        var rangeType = $("#universalsearch-rangetype-permanent").text().trim();
        var query = $("#universalsearch-query-permanent").text().trim();

        var params = {
            "rangetype": rangeType,
            "q": query,
            "field": field
        }

        switch(rangeType) {
            case "relative":
                params["relative"] = $("#universalsearch-relative-permanent").text();
                break;
            case "absolute":
                params["from"] = $("#universalsearch-from-permanent").text();
                params["to"] = $("#universalsearch-to-permanent").text();
                break;
            case "keyword":
                params["keyword"] = $("#universalsearch-keyword-permanent").text();
                break;
        }

        if(!!container.attr("data-stream-id")) {
            params["stream_id"] = container.attr("data-stream-id");
        }

        $.ajax({
            url: appPrefixed('/a/search/fieldterms'),
            data: params,
            success: function(data) {
                $(".terms-total", quickvalues).text(data.total);
                $(".terms-other", quickvalues).text(data.other);
                $(".terms-missing", quickvalues).text(data.missing);

                // Remove all items before writing again.
                $(".terms tbody", quickvalues).empty();
                $(".terms-distribution", quickvalues).empty();

                $(".terms-distribution", quickvalues).show();

                var colors = d3.scale.category20c();

                sortedKeys = Object.keys(data.terms).sort(function(a,b){return data.terms[b] - data.terms[a]});

                for(var i = 0; i < sortedKeys.length; i++){
                    var key = sortedKeys[i];
                    var val = data.terms[key];

                    var percent = (val/data.total*100);

                    var searchLink = "<a href='#' title='Search for this value. (Press alt to search immediately, shift to negate)' class='search-link' data-field='" + htmlEscape(field) + "' data-value='" + htmlEscape(key) + "'><i class='icon icon-search'></i></a>";

                    $(".terms tbody", quickvalues).append("<tr data-i='" + i + "' data-name='" + htmlEscape(key) + "'><td>"+ searchLink +"</td><td>" + htmlEscape(key) + "</td><td>" + percent.toFixed(2) + "%</td><td>" + htmlEscape(val) + "</td></tr>");
                    $(".terms-distribution", quickvalues).append("<div class='terms-bar terms-bar-" + i + "' style='width: " + percent + "%; background-color: " + colors(i) + ";'></div>");
                }
            },
            error: function(data) {
                quickvalues.hide();
                showError("Could not load quick values.");
            },
            complete: function() {
                $(".nano", quickvalues).nanoScroller();

                if (reload) {
                    // Loading complete. Set autoreload button to old color again.
                    $(".quickvalues-autorefresh", quickvalues).removeClass("loading");

                    // Call everything again in 2.5sec
                    setTimeout(function() {
                        showQuickValues(field, container, false, calculateDirection(button), true);
                    }, 3000)
                }
            }
        });
    }

    // Quickterms table row highlighting.
    $(".quickvalues .terms tbody tr")
        .live("mouseenter", highlightTermsBar)
        .live("mouseleave", resetTermsBar);

    function highlightTermsBar() {
        var bar = $(".terms-bar-" + $(this).attr("data-i"));
        bar.attr("data-original-color", bar.css("background-color"));
        bar.css("background-color", "#dd514c");
    }

    function resetTermsBar() {
        var bar = $(".terms-bar-" + $(this).attr("data-i"));
        bar.css("background-color", bar.attr("data-original-color"));
    }

    function calculateDirection(linkel) {
        if (($(window).height() - linkel.offset().top + $(window).scrollTop()) < 400) {
            return "up";
        } else {
            return "down";
        }
    }

    // Update all the positions, all the time.
    // Updating total event counts;
    (function updateAllPositions() {
        $(".quickvalues:visible").each(function(i) {
            var button = $(".analyze-field .show-quickvalues[data-field='" + $(this).attr("data-field") + "']");
            var direction = calculateDirection(button);

            updatePosition(button, $(this), direction);
        });

        setTimeout(updateAllPositions, 500);
    })();

    function updatePosition(button, quickvalues, direction) {
        var left = button.offset().left-$(window).scrollLeft()-622;

        switch(direction)  {
            case "up":
                var top = button.offset().top-$(window).scrollTop()-355;
                quickvalues.removeClass("quickvalues-down");
                quickvalues.addClass("quickvalues-up");
                break;
            case "down":
                var top = button.offset().top-$(window).scrollTop()-20;
                quickvalues.removeClass("quickvalues-up");
                quickvalues.addClass("quickvalues-down");
                break;
        }

        quickvalues.css("top", top);
        quickvalues.css("left", left);
    }

});
