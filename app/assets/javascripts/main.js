$(document).ready(function() {

    ZeroClipboard.config( { swfPath: appPrefixed("/assets/images/ZeroClipboard.swf"), forceHandCursor: true } );
    clipBoardClient = new ZeroClipboard($(".copy-clipboard"));

    clipBoardClient.on( 'mouseover', function(client, args) {
        $(this)
            .attr('data-original-title', $(this).attr('data-initial-title'))
            .tooltip({delay: { show: 0, hide: 0 }})
            .tooltip('fixTitle')
            .tooltip('show');
    });
    clipBoardClient.on( 'mouseout', function(client, args) {
        $(this).tooltip('hide');
    });
    clipBoardClient.on( 'complete', function(client, args) {
        $(this).tooltip('destroy');
        $(this).attr('data-original-title', "Copied.")
            .tooltip({delay: { show: 0, hide: 250 }})
            .tooltip('fixTitle')
            .tooltip('show');
    });

    // Call resizedWindow() only at end of resize event so we do not trigger all the time while resizing.
    var resizeMutex;
    $(window).resize(function() {
        clearTimeout(resizeMutex);
        resizeMutex = setTimeout(function() {
            onResizedWindow();
        }, 200);
    });

    // Close a notification.
    $(".delete-notification").on("click", function() {
        if (!confirm("Really delete this notification?")) {
            return false;
        }

        var notificationContainer = $(this).parent();

        $.ajax({
            url: appPrefixed("/a/system/notifications/" + $(this).attr("data-notificationtype")),
            type: "DELETE",
            success: function(data) {
                notificationContainer.hide();
            },
            error: function() {
                showError("Could not delete notification.");
            }
        });
    });

    var $typeaheadFields = $('.typeahead-fields');
    if ($typeaheadFields.length > 0) {
        // Typeahead for message fields.
        $.ajax({
            url: appPrefixed('/a/system/fields'),
            success: function (data) {
                $typeaheadFields.typeahead({
                        hint: true,
                        highlight: true,
                        minLength: 1
                    },
                    {
                        name: 'fields',
                        displayKey: 'value',
                        source: substringMatcher(data.fields, 'value', 6)
                    });
            }
        });
    }

    // Update progress for systemjobs that provide it.
    if ($(".systemjob-progress").size() > 0) {
        (function updateSystemJobProgress() {
            $.ajax({
                url: appPrefixed('/a/system/jobs'),
                success: function(data) {
                    // Check if there is an element that is not in the response anymore.
                    // That would mean it's job has finished or was stopped.
                    $(".systemjob-progress").each(function() {
                        var id = $(this).attr("data-job-id");

                        var included = false;
                        data.jobs.forEach(function(job) {
                            if (id === job.id) {
                                included = true;
                            }
                        });

                        if (!included) {
                            $(this).removeClass("systemjob-progress");
                            $(".progress-bar", $(this)).css("width", "100%");
                            $(".progress-bar", $(this)).text("100% complete (success)");
                            $(".progress-bar", $(this)).removeClass("active");
                            $(".progress-bar", $(this)).removeClass("progress-bar-info");
                            $(".progress-bar", $(this)).addClass("progress-bar-success");
                            $(".finished", $(this)).show();
                        }
                    });

                    data.jobs.forEach(function(job) {
                       var el = $("#job-" + job.id);

                        // Only update those jobs that provide progress.
                        if (el.hasClass("systemjob-progress")) {
                            $(".progress .progress-bar", el).css("width", job.percent_complete + "%");
                            $(".progress .progress-bar", el).text(job.percent_complete + "% complete");
                        }
                    });
                },
                complete: function() {
                    setTimeout(updateSystemJobProgress, 1000);
                }
            });
        })();
    }

    // Pausing/Resuming of message processing.
    $(".change-message-processing").on("click", function() {
        var action = $(this).attr("data-action");

        if  (action == "pause") {
            if (!confirm("Really pause message processing?")) {
                return;
            }
        }

        $.ajax({
            url: appPrefixed("/a/system/processing/" + action),
            type: "PUT",
            data: { node_id: $(this).attr("data-node-id") },
            success: function(data) {
                document.location.href = appPrefixed("/system/nodes");
            },
            error: function() {
                showError("Could not " + action + " message processing.");
            }
        });
        return false;
    });

    // Open input configuration modal.
    $("#configure-input").on("click", function() {
        var inputType = $("#input-type").val();
        $('[data-inputtype="' + inputType + '"]').modal();
    });

    // Check input configuration according to provided plugin attributes.
    $(".launch-input").on("click", function(event) {
        var $editForm = $('[data-inputtype="' + $(this).attr("data-type") + '"]').closest('form');
        var validated = validate($editForm);
        if (!validated) {
            event.preventDefault();
        }

        return validated;
    });

    $(".update-input").on("click", function(event) {
        var $editForm = $('#edit-input-' + $(this).data("input-id")).closest('form');
        var validated = validate($editForm);
        if (!validated) {
            event.preventDefault();
        }

        return validated;
    });

    $("#create-user-form").on("submit", function (event) {
        var validated = validate($("#create-user-form"));
        if (!validated) {
            event.preventDefault();
        }

        return validated;
    });

    $("#edit-user-form").on("submit", function (event) {
        var validated = validate($("#edit-user-form"));
        if (!validated) {
            event.preventDefault();
        }

        return validated;
    });

    // Add static field to input.
    $(".input-list .add-static-field").on("click", function(event) {
        event.preventDefault();
        var modal = $(".input-add-static-field");
        modal.modal();

        $("input", modal).val("");
        $("form", modal).attr("action", appPrefixed("/system/inputs/" + $(this).attr("data-node-id") + "/" + $(this).attr("data-input-id") + "/staticfields"));
    });

    $(".input-list .add-static-field-global").on("click", function(event) {
        event.preventDefault();
        var modal = $(".input-add-static-field");
        modal.modal();

        $("input", modal).val("");
        $("form", modal).attr("action", appPrefixed("/system/inputs/" + $(this).attr("data-input-id") + "/staticfields"));
    });

    // Set the focus on the first element of modals
    $(".input-configuration.modal").on("shown.bs.modal", focusFirstFormInput);
    $(".input-add-static-field.modal").on("shown.bs.modal", focusFirstFormInput);
    $(".edit-input-configuration.modal").on("shown.bs.modal", focusFirstFormInput);


    // Validate static fields
    $(".new-static-field").on("click", function(e) {
        if (!validate("#new-static-field-form")) {
            e.preventDefault();
        }
    });

    // permission chooser
    $(".permission-select").chosen({search_contains:true, width:"350px", inherit_select_classes:true});

    // timezone chooser
    $(".timezone-select").chosen({search_contains:true, inherit_select_classes:true, allow_single_deselect:true});

    // session timeout "never" vs "explicit value"
    var toggleSessionTimeoutEditableState = function(neverTimeout){
        // toggle the disabled state of all input fields
        $(".session-timeout-fields").prop("disabled", neverTimeout);
    };
    $("#session-timeout-never").change(function(){
        var enabledState = $(this).is(":checked");
        toggleSessionTimeoutEditableState(enabledState)
    });
    toggleSessionTimeoutEditableState($("#session-timeout-never").is(":checked"));

    // Submit button confirmation.
    $('input[data-confirm], button[data-confirm], a[data-confirm]').on("click", function() {
        return confirm($(this).attr("data-confirm"));
    });

    // Show log level metrics.
    $(".trigger-log-level-metrics").on("click", function(e) {
        e.preventDefault();
        $(".loglevel-metrics[data-node-id='" + $(this).attr("data-node-id") + "']").toggle();
    });

    // Change subsystem log level.
    $(".subsystem .dropdown-menu a[data-level]").on("click", function(e) {
        e.preventDefault();

        var newLevel = $(this).attr("data-level");
        var subsystem = $(this).closest(".subsystem").attr("data-subsystem");
        var nodeId = $(this).closest(".subsystem").attr("data-node-id");

        var link = $(this);
        var dropdown = $(this).closest("ul.dropdown-menu");

        $.ajax({
            url: appPrefixed('/system/logging/node/' + encodeURIComponent(nodeId) + '/subsystem/' + encodeURIComponent(subsystem) + '/' + encodeURIComponent(newLevel) + ''),
            type: "PUT",
            success: function(data) {
                $("li", dropdown).removeClass("active");
                link.closest("li").addClass("active");
                $(".dropdown-toggle .loglevel-title", link.closest(".subsystem")).text(newLevel.capitalize());
                showSuccess("Log level of subsystem changed.");
            },
            error: function() {
                showError("Could not change log level of subsystem.");
            }
        });
    });

    $(".metrics-filter").on("keyup", function() {
        var val = $(this).val();

        $(".metric-list li").hide();
        $(".metric-list li").each(function(i) {
            if ($(this).attr("data-metricname").match(new RegExp(".*" + val + ".*", "gi"))) {
               $(this).show();
            }
        });
    });

    $(".metric-list li .name .open").on("click", function(e) {
        e.preventDefault();
        $('.metric-list li .metric[data-metricname="' + $(this).attr("data-metricname") + '"]').toggle();
    });

    $(".toggle-fullscreen").on("click", function(e) {
        e.preventDefault();
        $(document).toggleFullScreen();
    });

    function formatNumberWithDataFormat() {
        try {
            $(this).text(numeral($(this).text()).format($(this).attr("data-format")));
        } catch (e) {}
    }

    $(".number-format").each(formatNumberWithDataFormat);

    $(".moment-from-now").each(function() {
        $(this).text(moment($(this).text()).fromNow());
    });

    function momentHumanize() {
        $(this).text(moment.duration(parseInt($(this).text()), $(this).attr("data-unit")).humanize());
    }

    $(".moment-humanize").each(momentHumanize);

    $(".shard-routing .shards .shard").tooltip();

    $(".index-description .index-details").on("click", function(e) {
        e.preventDefault();

        var linkElem = $(this);
        var index = $(this).data('index-name');
        var icon = linkElem.children().first();
        var holderElem = $(".index-info-holder", linkElem.closest(".index-description"));

        if(icon.hasClass("fa-caret-right")) {
            icon.removeClass("fa-caret-right");
            icon.addClass("fa-caret-down");

            $.get(appPrefixed("/a/system/indices/index_info/" + index + "/partial"), function(data) {
                holderElem.html(data);
                // Format numbers that were just loaded into the html document
                $(".number-format", holderElem).each(formatNumberWithDataFormat);
                $(".moment-humanize").each(momentHumanize);
            }).done(function() {
                holderElem.show();
            });
        } else {
            icon.removeClass("fa-caret-down");
            icon.addClass("fa-caret-right");
            holderElem.hide();
        }
    });

    $(".closed-indices").on("click", function() {
        $("ul", $(this)).show();
        $(".show-indices", $(this)).hide();
        $(this).off("click");
        $(this).css("cursor", "auto");
    });

    $('table.indexer-failures').dynatable({
        dataset: {
            ajax: true,
            ajaxUrl: appPrefixed('/a/system/indices/failures/dynatable'),
            ajaxOnLoad: true,
            records: [],
            perPageDefault: 50
        },
        features: {
            sort: false,
            pushState: true,
            search: false
        }
    });

    // Make a word plural/singular based on input field value.
    $("input.pluralsingular").on("keyup", function() {
        var target = $("." + $(this).attr("data-pluralsingular"));

        if ($(this).val() == "1") {
            target.text(target.attr("data-singular"));
        } else {
            target.text(target.attr("data-plural"));
        }
    });

    $(".alerts").dynatable({
        inputs: {
            perPageText: "Per page: ",
            searchPlacement: "before",
            perPagePlacement: "after"
        },
        dataset: {
            perPageDefault: 10
        }
    });

    $(".alerts .condition-id").on("click", function(e) {
        e.preventDefault();
        $("html, body").animate({ scrollTop: 0 }, "fast");
        $(".alert-condition[data-condition-id=" + $(this).attr("data-condition-id") + "]").effect(
            "highlight", { duration: 2000 }
        );
    });

    // Super confirmations (tm)
    $("a[data-super-confirm]").on("click", function(e) {
        var text = $(this).attr("data-super-confirm") + " (Confirm by typing in \"" + $(this).attr("data-super-confirm-word") + "\")";
        var confirm = prompt(text);

        if (confirm === $(this).attr("data-super-confirm-word")) {
            return true;
        }

        return false;
    });

    function onResizedWindow(){
        redrawGraphs();
    }

    $("input.input-global-checkbox").on("click", function(event) {
        var form = $(this).closest("form");
        var nodeselector = $("select.input-node-selector", form);

        if ($(this).is(":checked")) {
            nodeselector.prop('disabled', true);
        } else {
            nodeselector.prop('disabled', false);
        }
    });

    $("a.trigger-input-connection-details").on("click", function(e) {
        e.preventDefault();

        var inputId = $(this).attr("data-input-id");

        $("div.global-input-connection-details[data-input-id="+inputId+"]").toggle();
    });

    $("a.trigger-input-io-details").on("click", function(e) {
        e.preventDefault();

        var inputId = $(this).attr("data-input-id");

        $("div.global-input-io-details[data-input-id="+inputId+"]").toggle();
    });

    $('input, textarea').placeholder();

    $(".node-state").tooltip();

    datetimeFields = $(".browser-datetime");
    if(datetimeFields.length > 0) {
        datetimeFields.each(function() {
            var currentDatetime = moment();
            $(this).attr("title", currentDatetime.format(momentHelper.DATE_FORMAT_ISO));
            $(this).attr("datetime", currentDatetime.format(momentHelper.DATE_FORMAT_ISO));
            $(this).text(currentDatetime.format(momentHelper.DATE_FORMAT_TZ));
        });
    }

    $("button.select-all").on("click", function(e){
        var targetName = $(this).data('target');
        var checkboxes = $("input[name=" + targetName + "]:checkbox");
        checkboxes.each(function(index, element) {
            $(element).prop("checked", true);
        });
    });
});

function showError(message) {
    toastr.error(message, "Error", {
        "debug": false,
        "positionClass": "toast-bottom-full-width",
        "onclick": null,
        "fadeIn": 300,
        "fadeOut": 1000,
        "timeOut": 7000,
        "extendedTimeOut": 1000
    });
}

function showWarning(message) {
    toastr.warning(message, "Attention", {
        "debug": false,
        "positionClass": "toast-bottom-full-width",
        "onclick": null,
        "fadeIn": 300,
        "fadeOut": 1000,
        "timeOut": 7000,
        "extendedTimeOut": 1000
    });
}

function showSuccess(message) {
    toastr.success(message, "Information", {
        "debug": false,
        "positionClass": "toast-bottom-full-width",
        "onclick": null,
        "fadeIn": 300,
        "fadeOut": 1000,
        "timeOut": 7000,
        "extendedTimeOut": 1000
    });
}

String.prototype.splice = function( idx, rem, s ) {
    return (this.slice(0,idx) + s + this.slice(idx + Math.abs(rem)));
};

String.prototype.capitalize = function() {
    return this.charAt(0).toUpperCase() + this.slice(1);
};

function htmlEscape(x) {
    return $('<div/>').text(x).html();
}

function delayedAjaxCallOnKeyup(el, callback, delay) {
    var timer = null;
    el.onkeyup = function() {
        if (timer) {
            window.clearTimeout(timer);
        }
        timer = window.setTimeout( function() {
            timer = null;
            callback();
        }, delay );
    };
    el.onblur = function() {
        callback();
    };
    el = null;
}

function redrawGraphs() {
    if (typeof resultHistogram !== "undefined") {
        resultHistogram.redrawResultGraph();
    }

    for (var field in fieldGraphs) {
        fieldGraphs[field].configure({ width: $(".field-graph-components > div.field-graph.rickshaw_graph:first").width() });
        fieldGraphs[field].render();
    }
}

function isNumber(n) {
    return !isNaN(parseFloat(n)) && isFinite(n);
}

function generateShortId() {
    return Math.random().toString(36).substr(2, 9);
}

function generateId() {
    var r = "";
    for(var i = 0; i < 4; i++) {
        r = r + generateShortId();
    }

    return r;
}

function focusFirstFormInput(container) {
    var parentElement = container;
    if (!(parentElement instanceof jQuery)) {
        parentElement = this;
    }
    $("input[type!=hidden],select", parentElement).not(".tt-hint").not(":disabled").first().focus();
}

function substringMatcher(possibleMatches, displayKey, limit) {
    if (typeof limit === 'undefined' || limit === 0) {
        limit = Number.MAX_VALUE;
    }
    return function findMatches(q, callback) {
        var matches = [];

        // code duplication is better than a shitty abstraction
        possibleMatches.forEach(function(possibleMatch) {
            if (matches.length < limit && possibleMatch.indexOf(q) === 0) {
                var match = {};
                match[displayKey] = possibleMatch;
                matches.push(match);
            }
        });

        possibleMatches.forEach(function(possibleMatch) {
            if (matches.length < limit && possibleMatch.indexOf(q) !== -1 && possibleMatch.indexOf(q) !== 0) {
                var match = {};
                match[displayKey] = possibleMatch;
                matches.push(match);
            }
        });

        callback(matches);
    };
}

// Animated change of numbers.
(function($) {
    $.fn.intChange = function(countTo) {
        return this.each(function() {
            var elem = $(this);
            var origStripped = elem.text().replace(/,/g, "");

            if (!isNumber(origStripped)) {
                elem.text(numeral(countTo).format("0,0"));
                return;
            }

             if (parseInt(elem.text()) !== countTo) {
                elem.text(numeral(countTo).format("0,0"));
            }
        });
    };
})(jQuery);

clipBoardClient = {};

// This is holding all field graphs.
fieldGraphs = {};

// All dashboards.
globalDashboards = {};

function appPrefixed(url) {
    return gl2AppPathPrefix + url;
}