$(document).ready(function(){
  setInterval("showNewMessagesBar()", 30000);

});
  
function showNewMessagesBar() {
  // Fetch count of new messages since page load.
  $.post("/api/get_new_message_count", {since: $('#pageload').html()}, function(data) {
    response = eval('(' + data + ')');

    if (response.status == 'success') {
      // Make sure to get a number.
      newCount = parseInt(response.payload);

      // Only do something if the new count is greater than 0. 
      if (newCount > 0) {
        // Hide the new messages bar
        $('#new-messages-bar').hide();
        
        // Display with new content again,
        $('#new-messages-bar').html("<a href='#' onclick='window.location.reload()'>" + newCount + " new messages</a>");
        $('#new-messages-bar').show();

        // Update title.
        document.title = 'Graylog2 (' + newCount + ')';
      }
    }
  });
}
