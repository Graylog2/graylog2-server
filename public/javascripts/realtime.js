if (!window.WebSocket) {
  alert("Your browser does not support WebSockets.");
}

var Realtime = new function() {

  this.init = function(target, token) {
    _ws = new WebSocket(target);
    _token = token;
    _ws.onopen = function(evt) { f_opened(evt, token) };
    _ws.onclose = function(evt) { f_closed(evt) };
    _ws.onmessage = function(evt) { f_message(evt) };
    _ws.onerror = function(evt) { f_error(evt) }
  }

  var f_opened = function(evt, token) {
    // Send token for authentication.
    evt.currentTarget.send(token);

    // Realtime server would have closed connection already if token was wrong.
    mark_as_connected();
    log("Successfully opened socket.")
  }

  var f_message = function(evt) {
    write_to_messages(format_and_escape_message(evt.data));
  }

  var f_closed = function(evt) {
    mark_as_disconnected();
    log("Socket has been closed.")
  }

  var f_error = function(evt) {
    log("Error")
  }

  var write_to_messages = function(what) {
    if ($("#realtime-messages li").length >= 200) {
      $("#realtime-messages li:last").remove();
    }

    $("#realtime-messages").prepend("<li>" + what + "</li>");
  }

  var log = function(what) {
    $("#realtime-debug-log").append("<li><span class='realtime-debug-log-date'>" + new Date + "</span> - "+ htmlEncode(what) + "</li>"); 
  }

  var mark_as_connected = function() {
    $("#realtime-status").html("Connected");
    $("#realtime-status").removeClass("realtime-status-disconnected");
    $("#realtime-status").addClass("realtime-status-connected");
  }

  var mark_as_disconnected = function() {
    $("#realtime-status").html("Disconnected");
    $("#realtime-status").removeClass("realtime-status-connected");
    $("#realtime-status").addClass("realtime-status-disconnected");
  }

  var format_and_escape_message = function(msg) {
    // Get position of first dash, which is the date separator.
    dash_pos = msg.indexOf("-");
    message = msg.substr(dash_pos+2);
    date = msg.substr(0, dash_pos-1);
    return "<span class='realtime-messages-message-date'>" + htmlEncode(date) + "</span> " + htmlEncode(message);
  }

}

$(document).ready(function(){

  $("#realtime-debug-log-open").bind("click", function() {
    $("#realtime-debug-log").show();
    $(this).hide();
  });

});
