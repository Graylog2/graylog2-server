// ZOMG my JS sucks but I don't care
  
$(document).ready(function(){

  Shell.init();
  Shell.listen();

});

var Shell = new function() {

  this.init = function() {
    _cmd = $("#shell-command-input");
    _uprompt = $(".shell-prompt");
    _shell = $("#shell");

    // Set command input to full width.
    resize_cmd();

    // Focus into command prompt.
    _cmd.focus();
  }

  this.listen = function() {
    _cmd.bind('keypress', function(e) {
      var code = (e.keyCode ? e.keyCode : e.which);
      if(code == 13) { // "Enter" key

        // Do nothing for empty input.
        if ($.trim(_cmd.val().trim()).length == 0) {
          return false;
        }

        process(_cmd.val());
        return false;
      }
    });
  }

  var process = function(cmd) {
    waiting_line();
    _cmd.attr("disabled", "disabled");

    $.ajax({
      type: "POST",
      url: "analytics/shell",
      data: { cmd : cmd },
      success: function(data) {
        result = eval('(' + data + ')');
        if (result.code == "success") {
          render_result(success(result.ms, result.content));
        } else {
          render_result(error(result.reason));
        }
        
        eternalize(); // Move command out of input into static text.
      },
      error: function(data) {
        render_result(error("Internal error."));
        
        eternalize(); // Move command out of input into static text.
      }
    });
  }

  var error = function(reason) {
    return {
      code: "error",
      reason: reason
    }
  }

  var success = function(ms, content) {
    return {
      code: "success",
      ms: result.ms,
      content: result.content
    }
  }

  var resize_cmd = function() {
    container_width = parseInt($('#shell-container').css('width'));
    _cmd.css("width", container_width-_uprompt.width()-30);
  }

  var eternalize = function() {
    _cmd.removeAttr("id");

    new_cmd();
  }

  var new_cmd = function() {
    $(".shell-wait").remove();
    prompt_html = "<span class=\"shell-prompt\">" + $(".shell-prompt").first().html() + "</span>";
    new_line("<li>" + prompt_html + " <input id=\"shell-command-input\" class=\"shell-command\" type=\"text\" spellcheck=\"false\"></input></li>");
    _cmd = $("#shell-command-input"); // XXX fuckery
    resize_cmd();
    _cmd.focus();
    Shell.listen();
  }

  var waiting_line = function() {
    new_line("<li class=\"shell-wait\"><img src=\"images/loading-shell.gif\" /> Calculating</li>");
  }

  var render_result = function(res) {
    if (res == undefined) {
      output("Internal error - Undefined result.");
      return;
    }

    if (res.code == "success") {
      output("Completed in " + res.ms + "ms");

      render_result_content(res);
    } else {
      output("Error: " + res.reason);
    }
  }

  var render_result_content = function(result) {
    $("#content-inner").html(result.content);
  }

  var output = function(content) {
    new_line("<li>" + timestamp() + " - " + content + "</li>");
  }

  var new_line = function(content) {
    _shell.append(content);
  }

  var timestamp = function() {
    d = new Date();
    return timef(d.getHours()) + ":" + timef(d.getMinutes()) + ":" + timef(d.getSeconds());
  }

  var timef = function(i) {
    if (i < 10) {
     i = "0" + i
    }

    return i;
  }

}
