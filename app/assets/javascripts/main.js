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
    });


	// Adding more fields to the message result table.
	$(".field-selector").bind("change", function() {
		hash = $(this).attr("data-field-hash");
		td = $(".result-td-" + hash);
		th = $("#result-th-" + hash);
        var fieldname = $(this).data("field-name");

        var details = $("table.messages tr.message-detail-row td");
        var message = $("table.messages tr.message-row td");
        var colspan = parseInt(details.attr("colspan"));

        if ($(this).is(':checked')) {
            // show field
            searchViewState.addField(fieldname);
			th.show();
			td.show();

            details.attr("colspan", colspan+1);
            message.attr("colspan", colspan+1);
		} else {
            // hide field
            searchViewState.removeField(fieldname);
            th.hide();
			td.hide();

            details.attr("colspan", colspan-1);
            message.attr("colspan", colspan-1);
        }
	});


    // initialize searchViewState from query fragment if present (for bookmarks and link sharing of a current page)
    (function(){
        var uri = new URI();
        var fragment = uri.fragment(true);
        if (fragment["fields"] !== undefined) {
            if (fragment["fields"].length > 0) {
                var fields = fragment["fields"].split(",");

                var activeFields = 0;
                for (var i = 0; i < fields.length; i++) {
                    $(".field-selector[data-field-name="+fields[i]+"]").each(function(){
                        if (!$(this).is(":checked")) {
                            $(this).trigger("click"); // tick the checkbox if it wasn't checked before
                            activeFields+=1;
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
            $('.typeahead-fields').typeahead({
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
    $(".launch-input").on("click", function() {
        return validate('[data-inputtype="' + $(this).attr("data-type") + '"] form');
    });

    $(".update-input").on("click", function() {
        return validate('#edit-input-' + $(this).data("input-id") + ' form');
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
    $("ul.streams li.stream .trigger-stream-rules").on("click", function(e) {
        e.preventDefault();

        var rules = $('.streamrules-list-container[data-stream-id="' + $(this).closest("ul.streams li.stream").attr("data-stream-id") + '"]').find("div.streamrules-details");

        if (rules.is(":visible")) {
            rules.hide();
            $(".fa", this).removeClass("fa-caret-up");
            $(".fa", this).addClass("fa-caret-down");
            $("span", this).text("show rules");
        } else {
            rules.show();
            $(".fa", this).removeClass("fa-caret-down");
            $(".fa", this).addClass("fa-caret-up");
            $("span", this).text("hide rules");
        }
    });

    // Create a search on the fly.
    $(document).on("click", ".search-link", function(e) {
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
            if ($(this).attr("data-metricname").match(new RegExp(".*" + val + ".*", "gi"))) {
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
        
        $.get(appPrefixed("/a/system/indices/index_info/" + index + "/partial"), function(data) {
            var holderElem = $(".index-info-holder", linkElem.closest(".index-description"));
            holderElem.html(data);
            // Format numbers that were just loaded into the html document
            $(".number-format", holderElem).each(formatNumberWithDataFormat);
            $(".moment-humanize").each(momentHumanize);

            holderElem.toggle();
            
            var icon = linkElem.children().first();

            if(icon.hasClass("fa-caret-right")) {
                icon.removeClass("fa-caret-right");
                icon.addClass("fa-caret-down");
            } else {
                icon.removeClass("fa-caret-down");
                icon.addClass("fa-caret-right");
            }
        });

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

    function onResizedWindow(){
        redrawGraphs();
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

    $("table.messages tr.fields-row, table.messages tr.message-row").on("click", function() {
        var messageId = $(this).attr("data-message-id");
        $("table.messages tr.message-detail-row[data-message-id=" + messageId + "]").toggle();

        $("table.messages tbody[data-message-id=" + messageId + "]").toggleClass("message-group-toggled");
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