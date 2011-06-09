// ZOMG my JS sucks but I don't care

$(document).ready(function(){

  shell.init();
  shell.listen();

});

var shell = new function() {

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
        render_result(interpreter.cmd(_cmd.val()));
        eternalize(); // Move command out of input into static text.
        new_cmd();
        return false;
      }
    });
  }

  var resize_cmd = function() {
    margin_left = 20;
    container_width = parseInt($('#shell-container').css('width'));
    _cmd.css("width", container_width-_uprompt.width()-margin_left-20);
    _cmd.css("margin-left", margin_left);
  }

  var eternalize = function() {
    _cmd.attr("disabled", "disabled");
    _cmd.removeAttr("id");
  }

  var new_cmd = function() {
    prompt_html = "<span class=\"shell-prompt\">" + $(".shell-prompt").first().html() + "</span>";
    new_line(prompt_html + " <input id=\"shell-command-input\" class=\"shell-command\" type=\"text\"></input>");
    _cmd = $("#shell-command-input"); // XXX fuckery
    resize_cmd();
    _cmd.focus();
  }

  var render_result = function(res) {
    if (res.code == "success") {
      output("Completed in " + res.ms + "ms");
    } else {
      output("Error: " + res.reason);
    }
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

var interpreter = new function() {

  this.cmd = function(cmd) {
    return error("fuck this shit");
  }

  var error = function(reason) {
    return {
      code: "error",
      reason: reason
    };
  }

  var success = function(result) {
    return {
      code: "success",
      ms: 251,
      content: result
    }
  }

}
