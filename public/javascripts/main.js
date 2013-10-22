$(document).ready(function() {

	// Opening messages in sidebar with click in message result table.
	$(".messages tbody > tr").bind("click", function() {
		messageId = $(this).attr("data-message-id");
		index = $(this).attr("data-source-index");
		
		// Highlight message.
		$(".messages tbody > tr").removeClass("message-highlighted");
		$(this).addClass("message-highlighted");
		
		// Hide original sidebar and show ours again if it was already hidden before.
		$("#sidebar-original").hide();
		$("#sidebar-replacement").show();
		
		// Show loading spinner. Will be replaced onSuccess.
		spinner = "<h2><i class='icon-refresh icon-spin'></i> &nbsp;Loading message</h2>";
		$("#sidebar-replacement").html(spinner);
		
		$.get("/messages/" + index + "/" + messageId + "/partial", function(data) {
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
                    $.get("/a/analyze/" + index + "/" + messageId + "/message", function(data) {
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
	});
	
	// Date histogram resolution selector.
	$(".date-histogram-res-selector").each(function() {
		$(this).attr("href", "/search?" + addParameterToCurrentUrl("interval", $(this).attr("data-resolution")));
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

    // Always do this on first load.
    sizeSidebar();

	// Adding more fields to the message result table.
	$(".field-selector").bind("change", function() {
		hash = $(this).attr("data-field-hash");
		td = $(".result-td-" + hash);
		th = $("#result-th-" + hash);
		
		if ($(this).is(':checked')) {
			th.show();
			td.show();
		} else {
			th.hide();
			td.hide();
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
            url: "/a/system/notifications/" + $(this).attr("data-notificationtype"),
            type: "DELETE",
            success: function(data) {
                notificationContainer.hide();
            },
            error: function() {
                showError("Could not delete notification.");
            }
        });
    });

    // Stream rules.
    $("#new-stream-rule .sr-input").on("keyup change", function() {
        value = $(this).val();

        if (value != undefined && value != "") {
            // Selectbox options can have a custom replace string.
            s = $("option:selected", this);
            if (s != undefined && s.attr("data-reflect-string") != undefined && s.attr("data-reflect-string") != "") {
                value = s.attr("data-reflect-string");

                // Inverted?
                if ($("#sr-inverted").is(':checked')) {
                    value = "not " + value;
                }
            }
        } else {
            value = $(this).attr("placeholder");
        }

        $($(this).attr("data-reflect")).html(value);
    });

    // Stream rules inverter.
    $("#sr-inverted").on("click", function() {
        old_val = $("#new-stream-rule #sr-result-category").html();

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
        $("#new-stream-rule #sr-result-category").html(new_val);
    })

    // Add stream rule to stream rule list when saved.
    $("#add-stream-rule").on("click", function() {
        if (!validate("#sr")) {
            return false;
        }

        $("#stream-rules-placeholder").hide();

        rule = {
            field: $("#sr-field").val(),
            type: parseInt($("#sr-type").val()),
            value: $("#sr-value").val(),
            inverted: $("#sr-inverted-box").is(":checked")
        }
        // Add hidden field that is transmitted in form add visible entry.
        field = "<input type='hidden' name='rules[]' value='" + JSON.stringify(rule) + "' />"
        remover = "<a href='#' class='sr-remove'><i class='icon-remove'></i></a>";
        $("#stream-rules").append("<li>" + field + $("#sr-result").html().replace(/<(?:.|\n)*?>/gm, '') + " " + remover + "</li>");

        // Remove stream rule binding.
        $(".sr-remove").on("click", function() {
            $(this).parent().remove();
            return false;
        });

        $("#new-stream-rule").modal("hide");
    });

    // Typeahead for message fields.
    $.ajax({
        url: '/a/system/fields',
        success: function(data) {
            $(".typeahead-fields").typeahead({ source: data.fields, items: 6 });
        }
    });

    // Update progress for systemjobs that provide it.
    if ($(".systemjob-progress").size() > 0) {
        (function updateSystemJobProgress() {
            $.ajax({
                url: '/a/system/jobs',
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
            url: "/a/system/processing/" + action,
            type: "PUT",
            data: { node_id: $(this).attr("data-node-id") },
            success: function(data) {
                document.location.href = "/system/nodes";
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
    $(".input-list .add-static-field").on("click", function() {
        var modal = $(".input-add-static-field");
        modal.modal();

        $("input", modal).val("");
        $("form", modal).attr("action", "/system/inputs/" + $(this).attr("data-node-id") + "/" + $(this).attr("data-input-id") + "/staticfields");
    });

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

    var createUsernameField = $("form#create-user-form #username");
    if (createUsernameField.length) {
        var domElement = createUsernameField[0];
        delayedAjaxCallOnKeyup( domElement, function() {
            var username =  createUsernameField.val();
            $.ajax({
                url: "/a/system/users/" + encodeURIComponent(username),
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
                validationFailure( repeatPasswordField, "Passwords do not match!");
            }
        }, 150);
    }

    $(".delete-user-form").on("submit", function() {
        return confirm("Really remove user " + $(this).attr("data-username") + "?");
    });

    // Universalsearch validation.
    $("#universalsearch").on("submit", function() {
        return validate("#universalsearch");
    });

    // Submit button confirmation.
    $('button[data-confirm], a[data-confirm]').on("click", function() {
        return confirm($(this).attr("data-confirm"));
    });

    // Paginator disabled links should not trigger anything.
    $(".pagination .disabled a").on("click", function() {
       return false;
    });

    // Show fine-grained log level controls.
    $(".trigger-fine-log-level-controls").on("click", function() {
        $(".fine-log-level-controls[data-node-id='" + $(this).attr("data-node-id") + "']").toggle();
    });

    // Show log level metrics.
    $(".trigger-log-level-metrics").on("click", function() {
        $(".loglevel-metrics[data-node-id='" + $(this).attr("data-node-id") + "']").toggle();
    });

    // Check all fine-grained node log level checkboxes.
    $(".fine-log-level-controls .select-all").on("click", function() {
        var checkboxes = $(".fine-log-level-controls[data-node-id='" + $(this).attr("data-node-id") + "'] input[type=checkbox]");
        // The checkbox is already changed when this event is fired so we do not need to invert the condition.
        checkboxes.prop("checked", checkboxes.prop("checked"));
    });

    // Create a search on the fly.
    $(".search-link").live("click", function(e) {
        e.preventDefault();

        var field = $(this).attr("data-field");
        var value = $(this).attr("data-value");

        // Check if both required fields are properly set.
        if (field == undefined || value == undefined || field == "" || value == "") {
            return;
        }

        // escape common lucene special characters: + - && || ! ( ) { } [ ] ^ " ~ * ? : \
        value = value.replace(/\\/, "\\\\", "g"); // this one must be on top to avoid double-escaping lol
        value = value.replace(/\+/, "\\+", "g");
        value = value.replace(/-/, "\\-", "g");
        value = value.replace(/!/, "\\!", "g");
        value = value.replace(/\\^/, "\\^", "g");
        value = value.replace(/"/, "\\\"", "g");
        value = value.replace(/~/, "\\~", "g");
        value = value.replace(/\*/, "\\*", "g");
        value = value.replace(/\?/, "\\?", "g");
        value = value.replace(/:/, "\\:", "g");
        value = value.replace(/\|\|/, "\\|\\|", "g");
        value = value.replace(/&&/, "\\&\\&", "g");
        value = value.replace(/\[/, "\\[", "g");
        value = value.replace(/\]/, "\\]", "g");
        value = value.replace(/\(/, "\\(", "g");
        value = value.replace(/\)/, "\\)", "g");
        value = value.replace(/\{/, "\\}", "g");
        value = value.replace(/\}/, "\\}", "g");

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

            // If the query is "*", replace it fully. Makes no sense to generate "* AND foo:bar". (even though it would work)
            if ($.trim(originalQuery) == "*" || $.trim(originalQuery) == "") {
                query.val(ourQuery);
            } else {
                query.val(originalQuery + " AND " + ourQuery)
            }
        }
    });

    $("#scroll-to-search-hint, #scroll-to-search-hint i").on("click", function() {
        $("html, body").animate({ scrollTop: 0 }, "fast");
    });

    $("#global-throughput").on("click", function() {
        window.location.href = "/system";
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
        drawResultGraph();

        for (var field in fieldGraphs) {
            fieldGraphs[field].configure({ width: $("#main-content").width()-12 });
            fieldGraphs[field].render();
        }
    }
	
});

function searchDateTimeFormatted(date) {
    var day = ('0' + date.getDate()).slice(-2); // wtf javascript. this returns the day.
    var month = ('0' + (date.getMonth() + 1)).slice(-2);
    var year = date.getFullYear();

    var hour = ('0' + date.getHours()).slice(-2);
    var minute = ('0' + date.getMinutes()).slice(-2);
    var second = ('0' + date.getSeconds()).slice(-2);

    return year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
}

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
    $("#main-content").removeClass("span8");
    $("#main-content").addClass("span12");

    // Rebuild search result graph. (only doing something is there is one)
    drawResultGraph();
}

// This is holding all field graphs.
fieldGraphs = [];