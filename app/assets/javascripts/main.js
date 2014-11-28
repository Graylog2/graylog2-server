$(document).ready(function() {

    ZeroClipboard.config( { moviePath: appPrefixed("/assets/images/ZeroClipboard.swf") } );
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

    Mousetrap.bind('>', function() {
        if ($(".messages").size() == 0) {
            return;
        }
        if (e.preventDefault) {
            e.preventDefault();
        } else {
            // internet explorer
            e.returnValue = false;
        }

        var row = $(".messages tbody > tr.message-highlighted");
        var nextRow;
        if (row != undefined && row.size() > 0) {
            nextRow = row.closest('tr').next();
        } else {
            nextRow = $(".messages tbody tr").first();
        }


        $('html,body').animate({ scrollTop: nextRow.offset().top - ( $(window).height() - nextRow.outerHeight(true) ) / 2  }, 200);

        messageId = nextRow.attr("data-message-id");
        index = nextRow.attr("data-source-index");
        displayMessageInSidebar(nextRow, messageId, index);
    });

    Mousetrap.bind('<', function() {
        if ($(".messages").size() == 0) {
            return;
        }
        if (e.preventDefault) {
            e.preventDefault();
        } else {
            // internet explorer
            e.returnValue = false;
        }

        var row = $(".messages tbody > tr.message-highlighted");
        var prevRow = row.closest('tr').prev();

        $('html,body').animate({ scrollTop: prevRow.offset().top - ( $(window).height() - prevRow.outerHeight(true) ) / 2  }, 200);

        messageId = prevRow.attr("data-message-id");
        index = prevRow.attr("data-source-index");
        displayMessageInSidebar(prevRow, messageId, index);
    });

    function displayMessageInSidebar(row, messageId, index) {
        // Highlight message.
        $(".messages tbody > tr").removeClass("message-highlighted");
        $(row).addClass("message-highlighted");

        // Hide original sidebar and show ours again if it was already hidden before.
        $("#sidebar-original").hide();
        $("#sidebar-replacement").show();

        // Show loading spinner. Will be replaced onSuccess.
        spinner = "<h2><i class='icon-refresh icon-spin'></i> &nbsp;Loading message</h2>";
        $("#sidebar-replacement").html(spinner);

        $.get(appPrefixed("/messages/" + index + "/" + messageId + "/partial"), function(data) {
            $("#sidebar-replacement").html(data);
        })

            .fail(function() { displayFailureInSidebar("Sorry, could not load message."); })

            .complete(function() {

                sizeSidebar();

                // Inject terms of a message when modal is requested.
                $('.terms-msg-modal').on('show', function() {
                    messageId = $(this).attr("data-msg-id");
                    spinner = $("#terms-msg-" + messageId + " .modal-body .spinner");
                    list = $("#terms-msg-" + messageId + " .modal-body ul");
                    list_link = $("#terms-msg-" + messageId + "-as-list");

                    if ($(this).attr("data-loaded") != "true") {
                        $.get(appPrefixed("/a/analyze/" + index + "/" + messageId + "/message"), function(data) {
                            if (data.length > 0) {
                                for(var i = 0; i < data.length; i++) {
                                    list.append("<li>" + data[i] + "</li>");
                                }
                            } else {
                                list.append("<li>No terms extracted</li>")
                            }

                            // Hide spinner, show list link.
                            spinner.hide();
                            list_link.show();
                        });

                        // Mark as already loaded so we don't add the terms again on next open.
                        $(this).attr("data-loaded", "true");
                    }

                    // Show as list link.
                    list_link.bind("click", function() {
                        list.addClass("as-list");
                        $(this).hide();
                        return false;
                    });
                });
            })
    }

    // Opening messages in sidebar with click in message result table.
	$(".messages tbody > tr").bind("click", function() {
		messageId = $(this).attr("data-message-id");
		index = $(this).attr("data-source-index");

        displayMessageInSidebar(this, messageId, index);
	});

	// Go back in sidebar history / Show original sidebar.
	$(".sidebar-back").live("click", function() {
		$("#sidebar-replacement").hide();
		$("#sidebar-original").show();

		// Remove highlighting.
		$(".messages tbody > tr").removeClass("message-highlighted");

        // Set old sidebar to the correct height again.
        sizeSidebar();
	});

    // Hide sidebar completely.
    $(".sidebar-hide").live("click", function() {
        hideSidebar();
    });

    $(".sidebar-show").live("click", function() {
        showSidebar();
    });

    // Always do this on first load.
    sizeSidebar();

	// Adding more fields to the message result table.
	$(".field-selector").bind("change", function() {
		hash = $(this).attr("data-field-hash");
		td = $(".result-td-" + hash);
		th = $("#result-th-" + hash);
        var fieldname = $(this).data("field-name");
        if ($(this).is(':checked')) {
            searchViewState.addField(fieldname);
			th.show();
			td.show();
		} else {
            searchViewState.removeField(fieldname);
            th.hide();
			td.hide();
		}
	});


    // initialize searchViewState from query fragment if present (for bookmarks and link sharing of a current page)
    (function(){
        var uri = new URI();
        var fragment = uri.fragment(true);
        if (fragment["fields"] !== undefined) {
            if (fragment["fields"].length > 0) {
                var fields = fragment["fields"].split(",");
                for (var i = 0; i < fields.length; i++) {
                    $(".field-selector[data-field-name="+fields[i]+"]").each(function(){
                        if (!$(this).is(":checked")) {
                            $(this).trigger("click"); // tick the checkbox if it wasn't checked before
                        }
                    });
                }
            }
        }
    })();

    // turn on all pre-selected fields
    $(".field-selector[checked]").each(function() {
        searchViewState.addField($(this).data("field-name"));
    });

    $(".search-view-state").click(function(e) {
        var fields = searchViewState.getFieldsString();
        if (fields === undefined || fields.length === 0) {
            // don't add the fields parameter if nothing is field in
            return true;
        }
        // replace the href with our version containing the selected fields
        var href = $(this).attr("href");
        var uri = new URI(href);
        uri.removeQuery("fields");
        uri.addQuery("fields", fields);
        $(this).attr("href", uri.toString());
    });

    $(".fields-set-chooser").click(function(e) {
        e.preventDefault();
        var setName = $(this).data('fields-set');
        var fields = searchViewState.getFields();
        var i = 0;
        var field;

        switch (setName) {
            case "none":
                for (i = 0; i < fields.length; i++) {
                    field = fields[i];
                    $(".field-selector[data-field-name="+field+"]").each(function(){
                        if ($(this).is(":checked")) {
                            $(this).trigger("click");
                        }
                    });
                }
                break;
            case "default":
                // iterate over all selected fields, and turn them off, except if it's source or message
                for (i = 0; i < fields.length; i++) {
                    field = fields[i];
                    if (field === "source" || field === "message") {
                        // leave source and message turned on
                        continue;
                    }
                    $(".field-selector[data-field-name="+field+"]").each(function(){
                        if ($(this).is(":checked")) {
                            $(this).trigger("click");
                        }
                    });
                }
                // make sure source and message are on
                $("#field-selector-36cd38f49b9afa08222c0dc9ebfe35eb, #field-selector-78e731027d8fd50ed642340b7c9a63b3").each(function(){
                    if (!$(this).is(":checked")) {
                        $(this).trigger("click");
                    }
                });
                break;
            case "all":
                // for 'all' only toggle the page we are on, we don't need to toggle _all_ fields
                var selectedPage = $(".search-result-fields").attr("data-selected");
                $("." + selectedPage + " > .field-selector").each(function() {
                    // turn those fields on that aren't checked.
                    if (!$(this).is(":checked")) {
                        $(this).trigger("click");
                    }
                });
                break;
            default: console.log("Error, unknown fields set " + setName);
        }
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

    // Typeahead for message fields.
    $.ajax({
        url: appPrefixed('/a/system/fields'),
        success: function(data) {
            $(".typeahead-fields").typeahead({ source: data.fields, items: 6 });
        }
    });

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
                            $(".progress .bar", $(this)).css("width", "100%");
                            $(".progress", $(this)).removeClass("active");
                            $(".progress", $(this)).addClass("progress-success");
                            $(".finished", $(this)).show();
                        }
                    });

                    data.jobs.forEach(function(job) {
                       var el = $("#job-" + job.id);

                        // Only update those jobs that provide progress.
                        if (el.hasClass("systemjob-progress")) {
                            $(".progress .bar", el).css("width", job.percent_complete + "%");
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

    // Check input configuration according to provided plugin attribues.
    $(".launch-input").on("click", function() {
        return validate('[data-inputtype="' + $(this).attr("data-type") + '"] form');
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


    var focusFirstFormInput = function() {
        $("input[type!=hidden]", this).first().focus();
    };

    // Set the focus on the first element of modals
    $(".input-configuration.modal").on("shown", focusFirstFormInput);
    $(".input-add-static-field.modal").on("shown", focusFirstFormInput);


    // Remove static field.
    $(".input-list .static-fields ul li").on("mouseenter", function() {
        $(".remove-static-field", $(this)).show();
    }).on("mouseleave", function() {
        $(".remove-static-field", $(this)).hide();
    });

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

    $("#create-user-form").on("submit", function() {
        return validate("#create-user-form")}
    );

    $("#edit-user-form").on("submit", function() {
            return validate("#edit-user-form")}
    );

    var createUsernameField = $("form#create-user-form #username");
    if (createUsernameField.length) {
        var domElement = createUsernameField[0];
        delayedAjaxCallOnKeyup( domElement, function() {
            var username =  createUsernameField.val();
            $.ajax({
                url: appPrefixed("/a/system/users/" + encodeURIComponent(username)),
                type: "GET",
                cache: false,
                global: false,
                statusCode: {
                    204: function() {
                        validationFailure( createUsernameField, "Username is already taken.");
                        domElement.setCustomValidity('The entered user name is already taken.');
                    },
                    404: function() {
                        createUsernameField.popover("destroy");
                        domElement.setCustomValidity('');
                    }
                }
            });
        }, 150 );
    }

    var passwordField = $("form #password");
    if (passwordField.length) {
        var passwordDomElement = passwordField[0];
        delayedAjaxCallOnKeyup(passwordDomElement, function() {
            var password = passwordField.val();
            if (password.length < 6) {
                passwordDomElement.setCustomValidity("Password is too short!");
                validationFailure(passwordField, "Password is too short!");
            } else {
                passwordDomElement.setCustomValidity('');
                passwordField.popover("destroy");
            }
        }, 150);
    }

    var repeatPasswordField = $("form #password-repeat");
    if (repeatPasswordField.length) {
        var domElement1 = repeatPasswordField[0];
        delayedAjaxCallOnKeyup(domElement1, function() {
            var password = $("form #password").val();
            if (password == repeatPasswordField.val()) {
                domElement1.setCustomValidity('');
                repeatPasswordField.popover("destroy");
            } else {
                domElement1.setCustomValidity("Passwords do not match!");
                validationFailure(repeatPasswordField, "Passwords do not match!");
            }
        }, 150);
    }

    // Universalsearch validation.
    $("#universalsearch").on("submit", function() {
        return validate("#universalsearch");
    });

    // Submit button confirmation.
    $('input[data-confirm], button[data-confirm], a[data-confirm]').on("click", function() {
        return confirm($(this).attr("data-confirm"));
    });

    // Paginator disabled links should not trigger anything.
    $(".pagination .disabled a").on("click", function() {
       return false;
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

    // Show configured stream rules in streams list.
    $(".stream-row .trigger-stream-rules").on("click", function(e) {
        e.preventDefault();

        var rules = $('.streamrules-list-container[data-stream-id="' + $(this).closest(".stream-row").attr("data-stream-id") + '"]').find("div.streamrules-details");

        if (rules.is(":visible")) {
            rules.hide();
            $(".icon", this).removeClass("icon-caret-up");
            $(".icon", this).addClass("icon-caret-down");
            $("span", this).text("Show rules");
        } else {
            rules.show();
            $(".icon", this).removeClass("icon-caret-down");
            $(".icon", this).addClass("icon-caret-up");
            $("span", this).text("Hide rules");
        }
    });

    // Create a search on the fly.
    $(".search-link").live("click", function(e) {
        e.preventDefault();

        var field = $(this).attr("data-field");
        var value = $(this).attr("data-value");
        var operator = $(this).attr("data-search-link-operator") || "AND";

        // Check if both required fields are properly set.
        if (field == undefined || value == undefined || field == "" || value == "") {
            return;
        }

        // Replace newlines.
        value = value.replace(/\n/g, " ");
        value = value.replace(/<br>/g, " ");

        // If its a search phase we need to wrap it really good.
        if (value.indexOf(" ") >= 0) {
            value = "\"" + value + "\"";
        } else {
            // escape common lucene special characters: + - && || ! ( ) { } [ ] ^ " ~ * ? : \
            value = value.replace(/\\/g, "\\\\"); // this one must be on top to avoid double-escaping lol
            value = value.replace(/\//g, "\\/");
            value = value.replace(/\+/g, "\\+");
            value = value.replace(/-/g, "\\-");
            value = value.replace(/!/g, "\\!");
            value = value.replace(/\\^/g, "\\^");
            value = value.replace(/"/g, "\\\"");
            value = value.replace(/~/g, "\\~");
            value = value.replace(/\*/g, "\\*");
            value = value.replace(/\?/g, "\\?");
            value = value.replace(/:/g, "\\:");
            value = value.replace(/\|\|/g, "\\|\\|");
            value = value.replace(/&&/g, "\\&\\&");
            value = value.replace(/\[/g, "\\[");
            value = value.replace(/\]/g, "\\]");
            value = value.replace(/\(/g, "\\(");
            value = value.replace(/\)/g, "\\)");
            value = value.replace(/\{/g, "\\{");
            value = value.replace(/\}/g, "\\}");
        }

        var ourQuery = field + ":" + value;
        var query = $("#universalsearch-query");

        if (e.shiftKey) {
            // Shift key was pressed. Negate!
            ourQuery = "NOT " + ourQuery;
        }

        if (e.altKey) {
            // CTRL key was pressed. Search immediately!
            query.val(ourQuery);
            query.effect("bounce", { complete: function() {
                $("#universalsearch form").submit();
            }});
        } else {
            scrollToSearchbarHint();
            query.effect("bounce");
            var originalQuery = query.val();

            // if query already includes this one, do not add it!
            if (originalQuery.indexOf(ourQuery) >= 0) {
                return;
            }

            // If the query is "*", replace it fully. Makes no sense to generate "* AND foo:bar". (even though it would work)
            if ($.trim(originalQuery) == "*" || $.trim(originalQuery) == "") {
                query.val(ourQuery);
            } else {
                query.val(originalQuery + " " + operator + " " + ourQuery)
            }
        }
    });

    $("#scroll-to-search-hint, #scroll-to-search-hint i").on("click", function() {
        $("html, body").animate({ scrollTop: 0 }, "fast");
    });

    $(".metrics-filter").on("keyup", function() {
        var val = $(this).val();

        $(".metric-list li").hide();
        $(".metric-list li").each(function(i) {
            if ($(this).attr("data-metricname").match(new RegExp("^" + val + ".*", "g"))) {
               $(this).show();
            }
        });
    });

    $(".metric-list li .name .open").on("click", function(e) {
        e.preventDefault();
        $('.metric-list li .metric[data-metricname="' + $(this).attr("data-metricname") + '"]').toggle();
    });

    $("#global-throughput").on("click", function() {
        window.location.href = appPrefixed("/system");
    });

    $(".toggle-fullscreen").on("click", function(e) {
        e.preventDefault();
        $(document).toggleFullScreen();
    });

    $(".number-format").each(function() {
        $(this).text(numeral($(this).text()).format($(this).attr("data-format")));
    });

    $(".moment-from-now").each(function() {
        $(this).text(moment($(this).text()).fromNow());
    });

    $(".moment-humanize").each(function() {
        $(this).text(moment.duration(parseInt($(this).text()), $(this).attr("data-unit")).humanize());
    });

    $(".shard-routing .shards .shard").tooltip();

    $(".index-description .index-details").on("click", function(e) {
        e.preventDefault();

        $(".index-info", $(this).closest(".index-description")).toggle();
        var icon = $(this).children().first();

        if(icon.hasClass("icon-caret-right")) {
            icon.removeClass("icon-caret-right");
            icon.addClass("icon-caret-down");
        } else {
            icon.removeClass("icon-caret-down");
            icon.addClass("icon-caret-right");
        }
    });

    $(".nolink").on("live", function(e) {
        e.preventDefault();
    });

    $(".message-result-fields-range .page").on("click", function(e) {
        e.preventDefault();

        $(".search-result-fields li.search-result-field-type").hide();
        $(".search-result-fields li.page").show();

        $(".message-result-fields-search input").val("");

        $(".message-result-fields-range a").css("font-weight", "normal");
        $(".search-result-fields").attr("data-selected", "page");
        $(this).css("font-weight", "bold");
    });

    $(".message-result-fields-range .all").on("click", function(e) {
        e.preventDefault();

        $(".search-result-fields li.search-result-field-type").hide();
        $(".search-result-fields li.all").show();

        $(".message-result-fields-search input").val("");

        $(".message-result-fields-range a").css("font-weight", "normal");
        $(".search-result-fields").attr("data-selected", "all");
        $(this).css("font-weight", "bold");
    });

    $(".message-result-fields-search input").on("keyup", function(e) {
        var val = $(this).val();
        $(".search-result-fields li.search-result-field-type").hide();

        $(".search-result-fields li.search-result-field-type").each(function(i) {
            if ($(".field-name", $(this)).text().match(new RegExp("^" + val + ".*", "g"))) {
                if ($(this).hasClass($(".search-result-fields").attr("data-selected"))) {
                    $(this).show();
                }
            }
        });
    });

    $(".closed-indices").on("click", function() {
        $("ul", $(this)).show();
        $(".show-indices", $(this)).hide();
        $(this).off("click");
        $(this).css("cursor", "auto");
    });

    $(".sources-range").on("change", function() {
        var loc = new URI(window.location);
        loc.setQuery("range", $(this).val());
        window.location.href = loc.href();
    });

    $(".sources").dynatable({
        readers: {
            'messageCount': function(el, record) {
                return Number(el.innerHTML) || 0;
            }
        },
        inputs: {
            perPageText: "Per page: "
        },
        dataset: {
            perPageDefault: 50
        }
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

    // Show sort order icons on message table hover.
    $(".messages td, .messages th").on("mouseover", function() {
        $(".choose-sort-order", $(this).closest("table").find("th").eq($(this).index())).show();
    }).on("mouseout", function() {
        $(".choose-sort-order").hide();
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

    function scrollToSearchbarHint() {
        if ($(document).scrollTop() > 50) {
            $("#scroll-to-search-hint").fadeIn("fast").delay(1500).fadeOut("fast");
        }
    }

	function displayFailureInSidebar(message) {
		x = "<span class='alert alert-error sidebar-alert'><i class='icon-warning-sign'></i> " + message + "</span>"
		$("#sidebar-inner").html(x);
	}

	function addParameterToCurrentUrl(key, value) {
	    key = escape(key);
	    value = escape(value);

	    var kvp = document.location.search.substr(1).split('&');

	    var i = kvp.length;
	    var x;
	    while (i--)  {
	    	x = kvp[i].split('=');

	    	if (x[0]==key) {
	    		x[1] = value;
	    		kvp[i] = x.join('=');
	    		break;
	    	}
	    }

	    if (i<0) {
	    	kvp[kvp.length] = [key,value].join('=');
	    }

	    return kvp.join('&');
	}

    function sizeSidebar() {
        if ($("#sidebar .inner-content").filter(':visible').height() > $(window).height()-265-15) {
            // We need a scrollbar.
            $("#sidebar .nano").not(".quickvalues .nano").filter(':visible').css("height", $(window).height()-265-15);
            $("#sidebar .nano").not(".quickvalues .nano").filter(':visible').nanoScroller();
        } else {
            // No scrollbar required.
            $("#sidebar .nano").not(".quickvalues .nano").filter(':visible').css("height", $("#sidebar .inner-content").filter(':visible').height()+15);
        }
    }

    // Resize the sidebar regularly if window size changes.
    (function fixSidebarForWindow() {
        sizeSidebar();

        setTimeout(fixSidebarForWindow, 250);
    })();

    function onResizedWindow(){
        redrawGraphs();
    }

    // Set up numeral language.
    try {
        var browserLang = navigator.language || navigator.userLanguage;
        if (browserLang.indexOf("-") > 0) {
            var userShortLanguage = browserLang.substr(0, browserLang.indexOf("-"));
            numeral.language(userShortLanguage);
        } else {
            numeral.language(browserLang);
        }
    } catch(err) {
        numeral.language("en");
    }

    $(".remove-stream").on("click", function(event) {
        var result = confirm("Really delete stream?");
        if (result) {
            var elem = $(this).closest(".stream-row");
            var url = event.currentTarget.attributes["data-removeUrl"].value; // url already prefixed in template
            $.post(url, {}, function() {
                elem.fadeOut();
            });
        }
    });

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

    if ($.browser.safari) {
        $("form").on("submit", function(e) {
            $(this).find("input[required]").each(function(count, elem) {
                if ($(elem).val() === "") {
                    $(elem).addClass("required-input-highlight");
                    e.preventDefault();
                }
            });
            return true;
        });

        $("input[required]").on("keydown", function(e) {
            $(this).removeClass("required-input-highlight");
        });
    }

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

function hideSidebar() {
    $("#sidebar").hide();
    $("#sidebar-activator").show();
    var mainContentElement = $("#main-content");
    mainContentElement.removeClass("span8");
    mainContentElement.addClass("span12");
    redrawGraphs();
}

function showSidebar() {
    $("#sidebar-activator").hide();
    $("#sidebar").show();
    var mainContentElement = $("#main-content");
    mainContentElement.removeClass("span12");
    mainContentElement.addClass("span8");
    redrawGraphs();
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

function originalUniversalSearchSettings(searchViewState) {
    var result = {};

    result.query =  $("#universalsearch-query-permanent").text().trim();
    result.rangeType = $("#universalsearch-rangetype-permanent").text().trim();

    switch(result.rangeType) {
        case "relative":
            result.relative = $("#universalsearch-relative-permanent").text().trim();
            break;
        case "absolute":
            result.from = $("#universalsearch-from-permanent").text().trim();
            result.to = $("#universalsearch-to-permanent").text().trim();
            break;
        case "keyword":
            result.keyword = $("#universalsearch-keyword-permanent").text().trim();
            break;
    }

    if (searchViewState) {
        result.fields = searchViewState.getFieldsString();
    }

    return result;
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

// Animated change of numbers.
(function($) {
    $.fn.animatedIntChange = function(countTo, duration) {
        return this.each(function() {
            var elem = $(this);
            var origStripped = elem.text().replace(/,/g, "");

            if (!isNumber(origStripped)) {
                elem.text(numeral(countTo).format("0,0"));
                return;
            }

            var countFrom = parseInt(origStripped);
            $({value: countFrom}).animate({value: countTo}, {
                easing: "linear",
                duration: duration,
                step: function() {
                    elem.text(numeral(Math.floor(this.value)).format("0,0"));
                },
                complete: function() {
                    if (parseInt(elem.text()) !== countTo) {
                        elem.text(numeral(countTo).format("0,0"));
                    }
                }
            });
        });
    };
})(jQuery);

clipBoardClient = {};

// This is holding all field graphs.
fieldGraphs = {};

// All dashboards.
globalDashboards = {};

// contains selected fields etc
searchViewState = {
    fields: {},

    addField: function(name) {
        this.fields[name] = true;
        this.updateFragment();
    },
    removeField: function(name) {
        delete this.fields[name];
        this.updateFragment();
    },
    setSelectedFields: function(fieldsArray) {
        // reset the fields first
        this.fields = {};
        for (var idx = 0; idx < fieldsArray.length; idx++) {
            this.fields[fieldsArray[idx]] = true;
        }
        this.updateFragment();
    },
    getFields: function() {
        return Object.keys(this.fields);
    },
    getOrderedFields: function() {
        // order the fields as shown on the messages table
        var fields = this.getFields().sort(function(a, b){
            var tableHeaders = $("table.messages th[id^=result-th]:visible");
            for (i=0; i < tableHeaders.length; i++) {
                var content = $(tableHeaders[i]).text();
                var cleanContent = content.trim().toLowerCase();
                if (cleanContent == a) {
                    return -1;
                }
                if (cleanContent == b) {
                    return 1;
                }
            }
            return 0;
        });
        return fields;
    },
    getFieldsString: function() {
        return this.getOrderedFields().join(",");
    },
    updateFragment: function() {
        var uri = new URI();
        uri.fragment({fields: this.getFieldsString()});
        document.location.href = uri.toString();
    }
};

function appPrefixed(url) {
    return gl2AppPathPrefix + url;
}