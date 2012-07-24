/*global QUnit:false, module:false, test:false, asyncTest:false, expect:false*/
/*global start:false, stop:false ok:false, equal:false, notEqual:false, deepEqual:false*/
/*global notDeepEqual:false, strictEqual:false, notStrictEqual:false, raises:false*/
/*global sinon:false*/
(function($) {

  module("jQuery#buildShell", {
    setup: function() {
      this.elem = $("#shell-container");

      this.enterText = function(text) {
        $("#shell-command-input").val(text);
        var e = $.Event("keyup");
        e.which = 13;
        $("#shell-command-input").trigger(e);
      };
    },
    teardown: function() {
      if (this.ajaxStub) {
        this.ajaxStub.restore();
      }
    }
  });

  test("is chainable", 1, function() {
    // Not a bad test to run on collection methods.
    strictEqual(this.elem.shell(), this.elem, "should be chaninable");
  });

  test("scales the input", 1, function() {
    var spacer = 30;
    var initialwidth = $("#shell-container").find("input").outerWidth(true);
    this.elem.shell();
    strictEqual($("#shell-container").find("input").outerWidth(true)>initialwidth, true, "should be scaling the input width");
  });

  test("should be focussed", 1, function() {
    this.elem.shell();
    /* no :focus in jQuery 1.4.2 */
    strictEqual($("#shell-command-input")[0], $("#shell-command-input")[0].ownerDocument.activeElement, "input should be focussed");
  });

  test("input is emptied after pressing enter", 2, function() {
  this.ajaxStub = sinon.stub($, "ajax").returns(true);

    this.elem.shell();
    $("#shell-command-input").val('graylog');
    strictEqual($("#shell-command-input").val(), 'graylog', "input should be graylog");

    var e = $.Event("keyup");
    e.which = 13;

    $("#shell-command-input").trigger(e);
    strictEqual($("#shell-command-input").val(), "", "input should be emptied after pressing enter");
  });

  test("empty input does not create new lines in shell", 1, function() {
    this.elem.shell();
    this.enterText("");
    this.enterText("");

    strictEqual($(".shell-old-input").length, 0, "2 .shell-old-input elements");
  });

  test("arrow key up should give the last command", 2, function() {
    this.ajaxStub = sinon.stub($, "ajax").returns(true);

    this.elem.shell();
    $("#shell-command-input").val('graylog');
    strictEqual($("#shell-command-input").val(), "graylog", "input should be graylog");

    var e = $.Event("keyup");
    e.which = 13;
    $("#shell-command-input").trigger(e);

    e = $.Event("keyup");
    e.which = 38;
    $("#shell-command-input").trigger(e);

    strictEqual($("#shell-command-input").val(), "graylog", "input should be graylog then");
  });

  test("the shell history is 100 lines: integrational test", 4, function() {
    this.ajaxStub = sinon.stub($, "ajax").returns(true);

    this.elem.shell();
    var e = $.Event("keyup"),
        $input = $("#shell-command-input"),
        i;

    for (i = 0; i < 100; i++) {
      this.enterText(i);
    }

    e.which = 38;
    $input.trigger(e);

    strictEqual($input.val(), "99");

    $input.trigger(e);
    $input.trigger(e);
    $input.trigger(e);

    strictEqual($input.val(), "96");

    for (i = 0; i < 100; i++) {
      $input.trigger(e);
    }
    strictEqual($input.val(), "0");

    $input.trigger(e);
    $input.trigger(e);
    strictEqual($input.val(), "0");
  });

  test("the shell history: arrow down goes backward in history: integrational test", 4, function() {
    this.ajaxStub = sinon.stub($, "ajax").returns(true);

    this.elem.shell();
    var e = $.Event("keyup"),
        $input = $("#shell-command-input"),
        i;

    for (i = 0; i < 100; i++) {
      this.enterText(i);
    }

    e.which = 38;
    $input.trigger(e);

    strictEqual($input.val(), "99");

    $input.trigger(e);
    $input.trigger(e);
    $input.trigger(e);

    strictEqual($input.val(), "96");

    e.which = 40;

    $input.trigger(e);
    $input.trigger(e);
    $input.trigger(e);

    strictEqual($input.val(), "99");
    $input.trigger(e);
    strictEqual($input.val(), "99");
  });

  test("_handleHistory() sets up to 100 elements", 1, function() {
    this.elem.shell();
    var instance = $.data(this.elem[0], "shell"),
        testinput = [],
        i;

    for (i = 0; i <= 100; i++) {
      instance._handleHistory(i);
      testinput[i] = i;
    }

    strictEqual(instance.history[99], 99);
  });


  test("a loading div is shown after calling processInput", 1, function() {
    this.ajaxStub = sinon.stub($, "ajax").returns(true);

    this.elem.shell();
    var instance = $.data(this.elem[0], "shell");
    instance._processInput();

    strictEqual($(".shell-loading").length, 1, "a loading status should be shown then");
  });

  test("inputfield is disabled first", 1, function() {
    this.ajaxStub = sinon.stub($, "ajax").returns(true);

    this.elem.shell();
    var instance = $.data(this.elem[0], "shell");
    instance._processInput();

    strictEqual($("#shell-command-input").attr("disabled"), true);
  });

  test("input 'clear' clears the shell", 2, function() {
    this.ajaxStub = sinon.stub($, "ajax").returns(true);

    this.elem.shell();
    this.enterText("graylog");
    this.enterText("test");
    this.enterText("clear");

    strictEqual($(".shell-old-input").length, 0, "no .shell-old-input elements");
    strictEqual($(".shell-wait").length, 0, "no .shell-wait elements");
  });

  test("new input shows up as 'shell-old-input' after submitting by pressing enter", 1, function() {
    this.ajaxStub = sinon.stub($, "ajax").returns(true);

    this.elem.shell();
    var instance = $.data(this.elem[0], "shell");
    instance._processInput();

    strictEqual($("#shell-oldinput-container").find(".shell-old-input").length, 1, "2 .shell-old-input elements");
  });

  test("if history is disabled, new input shows NOT up after submitting by pressing enter", 1, function() {
    this.ajaxStub = sinon.stub($, "ajax").returns(true);

    this.elem.shell({history: false});
    this.enterText("graylog");
    this.enterText("test");

    strictEqual($(".shell-old-input").length, 0);
  });

  test("jQuery ajax should be called after pressing enter", 3, function() {
    this.ajaxStub = sinon.stub($, "ajax").returns(true);

    this.elem.shell();
    this.enterText("test");

    ok($.ajax.calledOnce);
    strictEqual($.ajax.getCall(0).args[0].url, "analytics/shell");
    strictEqual($.ajax.getCall(0).args[0].dataType, "json");
  });

  test("render callback should be called if a error happens", 2, function() {
    var errorMsg = {code: "error", reason: "Internal error."};

    this.ajaxStub = sinon.stub($, "ajax").yieldsTo("error", []);

    this.elem.shell();
    var instance = $.data(this.elem[0], "shell");

    this.spy(instance, "_renderCallback");

    this.enterText("test");

    ok(instance._renderCallback.calledOnce);
    deepEqual(instance._renderCallback.getCall(0).args[0], errorMsg);
  });

  test("if renderCallback() gets no data, it will print an error", 2, function() {
    this.elem.shell();
    var instance = $.data(this.elem[0], "shell");

    instance._renderCallback();

    strictEqual($('#shell').find('.shell-error').length, 1);
    strictEqual($('#shell').find('.shell-error').text(), "01:00:00 - Internal error - Undefined result."); // sinon qunit date is always 01:00
  });

  test("renderCallback() renders errors from the ajax-error callback", 2, function() {
    this.elem.shell();
    var instance = $.data(this.elem[0], "shell");

    instance._renderCallback({code: "error", reason: "Internal error."});

    strictEqual($('#shell').find('.shell-error').length, 1);
    strictEqual($('#shell').find('.shell-error').text(), "01:00:00 - Error: Internal error."); // sinon qunit date is always 01:00
  });

  test("renderCallback() renders count results", 3, function() {
    this.elem.shell();
    var instance = $.data(this.elem[0], "shell"),
        text = "42 is the answer to everything";

    instance._renderCallback({code: "success", ms: "20", op: "count", result: text});

    strictEqual($('#shell').find('.shell-success').length, 1);
    strictEqual($('#shell').find('.shell-success').text().match(text)[0], text);
    strictEqual($('#shell').find('.shell-success').text().match("Completed in 20ms")[0], "Completed in 20ms");
  });

  test("renderCallback() renders count results and 1 result has no comma at the end", 4, function() {
    this.elem.shell();
    var instance = $.data(this.elem[0], "shell"),
        text = "42 is the answer to everything";

    instance._renderCallback({code: "success", ms: "20", op: "distinct", result: [text]});

    strictEqual($('#shell').find('.shell-success').length, 1);
    strictEqual($('#shell').find('.shell-success').text().match(text)[0], text);
    strictEqual($('#shell').find('.shell-success').text().match("Completed in 20ms")[0], "Completed in 20ms");
    strictEqual($('#shell').find('.shell-success').text().split(",").length, 1);
  });

  test("renderCallback() renders 3 distinct results and the results contain 2 comma", 5, function() {
    this.elem.shell();
    var instance = $.data(this.elem[0], "shell"),
        text = "42 is the answer to everything";

    instance._renderCallback({code: "success", ms: "20", op: "distinct", result: [text, text, text]});

    strictEqual($('#shell').find('.shell-success').length, 1);
    strictEqual($('#shell').find('.shell-success').text().match(text)[0], text);
    strictEqual($('#shell').find('.shell-success').text().match("Completed in 20ms")[0], "Completed in 20ms");
    strictEqual($('#shell').find('.shell-success').text().split(",").length, 3);
    strictEqual($('#shell').find('.shell-success').text().match("No matches."), null);
  });

  test("renderCallback() gets distinct results count from 0 and the result says 'no matches.'", 4, function() {
    this.elem.shell();
    var instance = $.data(this.elem[0], "shell"),
        text = "No matches.";

    instance._renderCallback({code: "success", ms: "20", op: "distinct", result: []});

    strictEqual($('#shell').find('.shell-success').length, 1);
    strictEqual($('#shell').find('.shell-success').text().match(text)[0], text);
    strictEqual($('#shell').find('.shell-success').text().match("Completed in 20ms")[0], "Completed in 20ms");
    strictEqual($('#shell').find('.shell-success').text().split(",").length, 1);
  });

  test("renderCallback() gets distribution results count from 0 and the result says 'no matches.'", 4, function() {
    this.elem.shell();
    var instance = $.data(this.elem[0], "shell"),
        text = "No matches.";

    instance._renderCallback({code: "success", ms: "20", op: "distribution", result: []});

    strictEqual($('#shell').find('.shell-success').length, 1);
    strictEqual($('#shell').find('.shell-success').text().match(text)[0], text);
    strictEqual($('#shell').find('.shell-success').text().match("Completed in 20ms")[0], "Completed in 20ms");
    strictEqual($('#shell').find('.shell-success').text().split(",").length, 1);
  });

  test("renderCallback() renders 3 distribution results and the results contain 2 comma", 5, function() {
    this.elem.shell();
    var instance = $.data(this.elem[0], "shell"),
        item = {distinct: "42 is the answer to everything", count: 20};

    instance._renderCallback({code: "success", ms: "25", op: "distribution", result: [item, item, item]});

    strictEqual($('#shell').find('.shell-success').length, 1);
    strictEqual($('#shell').find('.shell-success').text().match(item.distinct)[0], item.distinct);
    strictEqual($('#shell').find('.shell-success').text().match("Completed in 25ms")[0], "Completed in 25ms");
    strictEqual($('#shell').find('.shell-success').text().split(",").length, 3);
    strictEqual($('#shell').find('.shell-success').text().match("No matches."), null);
  });

  test("renderCallback() renders findresult result", 2, function() {
    this.elem.shell();
    var instance = $.data(this.elem[0], "shell");

    instance._renderCallback({code: "success", op: "findresult", content: '<div id="bar">foo</div>'});

    strictEqual($('body').find('#bar').length, 1);
    strictEqual($('body').find('#bar').text(), "foo");
  });

  test("if the shell gets too long, old elements are removed", 1, function() {
    this.elem.shell();
    this.ajaxStub = sinon.stub($, "ajax").yieldsTo("success", {code: "success", ms: "20", op: "distribution", result: []});

    for (var i = 0; i < 20; i++) {
      this.enterText("test");
    }

    strictEqual($(".shell-old-input").length, 14);
  });

  test("calling the public .('focus') will refocus the input field", 2, function() {
    this.elem.shell();

    $('#another-input').focus();
    strictEqual($('#another-input')[0], $('#another-input')[0].ownerDocument.activeElement, "input should be focussed");

    this.elem.shell('focus');
    strictEqual($("#shell-command-input")[0], $("#shell-command-input")[0].ownerDocument.activeElement, "input should be focussed");
  });

}(jQuery));
