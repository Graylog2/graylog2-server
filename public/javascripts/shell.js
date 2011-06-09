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

        // Empty line for empty input.
        if ($.trim(_cmd.val().trim()).length == 0) {
          empty_line();
          return false;
        }

        render_result(Interpreter.cmd(_cmd.val()));
        eternalize(); // Move command out of input into static text.
        new_cmd();
        return false;
      }
    });
  }

  var resize_cmd = function() {
    container_width = parseInt($('#shell-container').css('width'));
    _cmd.css("width", container_width-_uprompt.width()-30);
  }

  var eternalize = function() {
    _cmd.attr("disabled", "disabled");
    _cmd.removeAttr("id");
  }

  var new_cmd = function() {
    prompt_html = "<span class=\"shell-prompt\">" + $(".shell-prompt").first().html() + "</span>";
    new_line(prompt_html + " <input id=\"shell-command-input\" class=\"shell-command\" type=\"text\" spellcheck=\"false\"></input>");
    _cmd = $("#shell-command-input"); // XXX fuckery
    resize_cmd();
    _cmd.focus();
    Shell.listen();
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

  var empty_line = function() {
    eternalize(); // Move command out of input into static text.
    new_cmd();
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

var Interpreter = new function() {

  this.cmd = function(command) {
    var _parsed_cmd = Parser.parse(command);

    if ((err = validate(_parsed_cmd)) != true) {
      return error(err);
    }

    return success();
  }

  var validate = function(what) {
    console.log(what);

    // Validate target.
    allowed_targets = [ "all", "streams" ];
    if (what.target == null || $.inArray(what.target, allowed_targets) == -1) {
      return "Invalid target";
    }

    // Validate type.
    allowed_types = [ "count", "find", "distinct" ];
    if (what.type == null || $.inArray(what.type, allowed_types) == -1) {
      return "Invalid type"
    }

    // All validations passed.
    return true;
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

var Parser = new function() {
  
  this.parse = function(command) {
    _command = command;

    return {
      target: target(),
      type: type(),
      //parameters: [
      //  [ "_http_response_code", "200" ],
      //  [ "_http_verb", "GET" ]
      //]
      parameters: parameters()
    }
  }

  var target = function() {
    return extract(/^(.+?)\./);
  }

  var type = function() {
    return extract(/^.+\.(.+?)\(/);
  }

  var parameters = function() {
    ps = extract(/\((.+)\)/);
    if (ps[0] != null) {
      // XXX continue here XXX XXX cut and trim parameters
    }
    return ps;
  }

  var extract = function(regex) {
    x = _command.match(regex);
    
    if(x == null || x[1] == null) {
      return null
    }
    
    return x[1];
  }

}
