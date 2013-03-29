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
			
			// Inject terms of a message when modal is requested.
			$('.terms-msg-modal').on('show', function() {
				messageId = $(this).attr("data-msg-id");
				spinner = $("#terms-msg-" + messageId + " .modal-body .spinner");
				list = $("#terms-msg-" + messageId + " .modal-body ul");
				list_link = $("#terms-msg-" + messageId + "-as-list");
				
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
				})
				
				// Show as list link.
				list_link.bind("click", function() {
					list.addClass("as-list");
					$(this).hide();
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
	});
	
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
	
});