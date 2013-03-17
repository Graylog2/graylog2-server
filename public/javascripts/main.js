$(document).ready(function() {

	$(".messages tbody > tr").bind("click", function() {
		messageId = $(this).attr("data-message-id");
		index = $(this).attr("data-source-index");
		
		// Show loading spinner. Will be replaced onSuccess.
		spinner = "<h2><i class='icon-refresh icon-spin'></i> &nbsp;Loading message</h2>";
		$("#sidebar-inner").html(spinner);
		
		$.get("/messages/" + index + "/" + messageId + "/partial", function(data) {
			$("#sidebar-inner").html(data);
		})
		.fail(function() { displayFailureInSidebar("Sorry, could not load message."); });
	});
	
	function displayFailureInSidebar(message) {
		x = "<span class='alert alert-error sidebar-alert'><i class='icon-warning-sign'></i> " + message + "</span>"
		$("#sidebar-inner").html(x);
	}
	
});