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
    _last_command = "";

    // Set command input to full width.
    resize_cmd();

    // Focus into command prompt.
    _cmd.focus();
  }

  this.listen = function() {
    _cmd.bind('keydown', function(e) {
      var code = (e.keyCode ? e.keyCode : e.which);
      if (code == 13) { // "Enter" key

        // Do nothing for empty input.
        if ($.trim($(this).val().trim()).length == 0) {
          return false;
        }

        process($(this).val());

        // No need to have "clear" in the history.
        if ($(this).val() != "clear") {
          _last_command = $(this).val();
        }

        return false;
      }

      // Show last command.
      if (code == 38) { // "Up arrow" key
        if ($(this).val != _last_command) {
          $(this).val(_last_command);
        }
      }
    });
  }

  var process = function(cmd) {
    if (cmd == "clear") {
      clear();
      return;
    }
    
    waiting_line();
    _cmd.attr("disabled", "disabled");

    $.ajax({
      type: "POST",
      url: "analytics/shell",
      data: { cmd : cmd },
      success: function(data) {
        result = eval('(' + data + ')');
        if (result.code == "success") {
          render_result(success(result.ms, result.content, result.op, result.result));
        } else {
          render_result(error(result.reason));
        }

        bindMessageSidebarClicks();

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

  var success = function(ms, content, op, result) {
    return {
      code: "success",
      ms: ms,
      content: content,
      op: op,
      result: result
    }
  }

  var resize_cmd = function() {
    container_width = parseInt($('#shell-container').css('width'), 10);
    _cmd.css("width", container_width-_uprompt.width()-30);
  }

  var eternalize = function() {
    _cmd.removeAttr("id");

    new_cmd();
  }

  var new_cmd = function(prompt_html) {
    $(".shell-wait").remove();
    
    if (prompt_html == undefined) {
      prompt_html = $(".shell-prompt").first().html();
    }

    prompt_html = "<span class=\"shell-prompt\">" + prompt_html + "</span>";
    new_line("<li>" + prompt_html + " <input id=\"shell-command-input\" class=\"shell-command\" type=\"text\" spellcheck=\"false\"></input></li>");

    // Check if there are more shell lines than allowed. Remove first if so. (to limit size)
    if ($("#shell li").length >= 15) {
      $("#shell li:first").remove();
      $("#shell li:first").remove();
    }

    _cmd = $("#shell-command-input"); // XXX fuckery
    _uprompt = $(".shell-prompt"); // XXX fuckery (because original prompt has possibly been removed)

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
      x = "Completed in " + res.ms + "ms";

      switch (res.op) {
        case "count":
          x += " - Count result: " + "<span class=\"shell-result-string\">" + htmlEncode(res.result) + "</span>";
          break;
        case "distinct":
          x += " - Distinct result: " + "<span class=\"shell-result-string\">";
          if (res.result.length == 0) {
            x += "No matches.";
          } else {
            for (key in res.result) {
              x += htmlEncode(res.result[key]) + ", ";
            }
            x = x.substring(0, x.length - 2); // Remove last comma and whitespace.
            x += "</span>"
          }
          break;
        case "distribution":
          x += " - Distribution result: " + "<span class=\"shell-result-string\">";
          if (res.result.length == 0) {
            x += "No matches.";
          } else {
            for (key in res.result) {
              x += htmlEncode(res.result[key]["distinct"]) + "(" + parseInt(res.result[key]["count"], 10) + "), ";
            }
            x = x.substring(0, x.length - 2); // Remove last comma and whitespace.
            x += "</span>"
          }
          break;
      }

      output(x);

      if (res.op == "find") {
        render_result_content(res);
      }
    } else {
      output("Error: " + htmlEncode(res.reason));
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

  var clear = function() {
    prompt_name = $(".shell-prompt").first().html(); // We need to cache that, because it's gonna be removed next.
    $("#shell li").remove();
    new_cmd(prompt_name);
  }

}
