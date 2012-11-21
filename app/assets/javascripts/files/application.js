$(document).ajaxSend(function(e, xhr, options) {
  var token = $("meta[name='csrf-token']").attr("content");
  xhr.setRequestHeader("X-CSRF-Token", token);
});

$(document).ready(function() {

    // Hide notifications after some time.
    setInterval(function() {
      $(".notification-flash").hide("drop");
    }, 3500);

    // Stream rule form.
    $('#streamrule_rule_type').bind('change', function() {
        $('.stream-value-field').hide();
        $('.stream-value-help').hide();
        $('.stream-value-field').attr("disabled", true);

        help = null;
        master = null;
        switch(this.value) {
            case '1':
                field = $('.stream-value-message');
                break;
            case '2':
                field = $('.stream-value-host');
                break;
            case '3':
                field = $('.stream-value-severity');
                break;
            case '4':
                field = $('.stream-value-facility');
                break;
            case '5':
                field = $('.stream-value-timeframe');
                help = $('#stream-value-timeframe-help');
                break;
            case '6':
                field = $('.stream-value-additional-field');
                help = $('#stream-value-additional-field-help');
                break;
            case '8':
                field = $('.stream-value-severity-or-higher');
                break;
            case '9':
                field = $('.stream-value-host-regex');
                notify("Remember to possibly escape characters in the regular expression." +
                       "A typical mistake is forgetting to escape dots in host names.");
                break;
            case '10':
                field = $('.stream-value-fullmessage');
                break;
            case '11':
                field = $('.stream-value-filename');
                notify("Remember to possibly escape characters in the regular expression." +
                       "A typical mistake is forgetting to escape the dot in filenames.");
                break;
            case '12':
                field = $('.stream-value-facility-regex');
                notify("Remember to possibly escape characters in the regular expression.");
                break;
        }
        field.removeAttr("disabled");
        field.show();

        if (help != null) { help.show(); }
    });

    // Stream Quick chooser
    $('#favoritestreamchooser_id').bind('change', function() {
       window.location = "/streams/show/" + parseInt(this.value, 10);
    });

    // Quickfilter
    $('#messages-show-quickfilter').bind('click', function() {
        var showLink = $('#messages-show-quickfilter');
        if (showLink.hasClass('messages-show-quickfilter-expanded')) {
            // Quickfilter is expanded. Small down on click.
            showLink.removeClass('messages-show-quickfilter-expanded');

            // Hide quickfilters.
            $('#messages-quickfilter').hide();
        } else {
            // Quickfilter is not expanded. Expand on click.
            showLink.addClass('messages-show-quickfilter-expanded');

            // Show quickfilters.
            $('#messages-quickfilter').fadeIn(800);
        }
    });

    // Full message view resizing.
    $('#messages-show-message-full').css('width', parseInt($('#content').css('width'))-15, 10);
    $('#messages-show-message-full').css('height', parseInt($('#messages-show-message-full').css('height'))+10, 10);

    // Visuals: Message spread permalink
    $('#visuals-spread-hosts-permalink-link').bind('click', function() {
      $('#visuals-spread-hosts-permalink-link').hide();
      $('#visuals-spread-hosts-permalink-content').show();
      return false;
    });

    $('#analytics-new-messages-update-range').numeric();
    // Visuals: Update of new messages graph.
    $('#analytics-new-messages-update-submit').bind('click', function() {
      i = $('#analytics-new-messages-update-range');
      v = parseInt(i.val(), 10);
      
      if (v <= 0) {
        return false;
      }

      // Possibly multiply if days or weeks was selected as range.
      if ($('#range_type_days').attr("checked")) {
        range_type = "days";
        range_num = v*24;
      } else if($('#range_type_weeks').attr("checked")) {
        range_type = "weeks";
        range_num = v*24*7;
      } else {
        range_type = "hours";
        range_num = v;
      }

      // Show loading message.
      $("#analytics-new-messages-update-loading").show();

      // Update graph.
      $.post($(this).attr("data-updateurl") + "&hours=" + range_num, function(response) {

        // Plot is defined inline. (I suck at JavaScript)
        plot(response.data);

        // Update title.
        $('#analytics-new-messages-range').html(v);
        $('#analytics-new-messages-range-type').html(range_type);

        // Hide loading message.
        $("#analytics-new-messages-update-loading").hide();
      }, "json");

      return false;
    });

    // Hide sidebar.
    $("#sidebar-hide-link").bind('click', function() {
      $("#main-right").hide();
      $("#main-left").animate({ width: '100%' }, 700);
      return false;
    });

    // Favorite streams: Sparklines.
    $(".favorite-stream-sparkline").sparkline(
      "html",
      {
        type: "line",
        width: "40px",
        lineColor: "#fd0c99",
        fillColor: "#fdd",
        spotColor: false,
        minSpotColor: false,
        maxSpotColor: false
      }
    );

    // Entity lists: Sparklines.
    $(".el-e-sparkline").sparkline(
      "html",
      {
        type: "line",
        width: "70px",
        height: "23px",
        lineColor: "#fd0c99",
        fillColor: "#fdd",
        spotColor: false,
        minSpotColor: false,
        maxSpotColor: false
      }
    );

    // AJAX trigger
    $(".ajaxtrigger").bind("click", function() {
      field = $(this);
      loading = $("#" + field.attr("id") + "-ajaxtrigger-loading");
      done = $("#" + field.attr("id") + "-ajaxtrigger-done");

      field.attr("disabled","disabled");
      done.hide();
      loading.show();
      $.post(field.attr("data-target"), function(data) {
        field.removeAttr("disabled");
        loading.hide();
        done.show();
      });
    });

    // Universal search field.
    var unisearch = $("#universal-search-field");
    var stdval = unisearch.attr("data-stdval");
    if (unisearch.val() == "") {
      unisearch.val(stdval);
    }
    unisearch.focus(function () {
      if (unisearch.val() == stdval) {
        unisearch.val("");
      }
    });
    unisearch.blur(function () {
      if (unisearch.val() == "") {
        unisearch.val(stdval);
      }
    });

    // Stream alerts: Inputs only numeric.
    $('#streams-alerts-limit').numeric();
    $('#streams-alerts-timespan').numeric();

    // Stream descriptions.
    $(".stream-description-change").bind("click", function() {
      $("#streams-description-text").hide();
      $("#streams-set-description").show();
    });

    // Awesome submit links
    $(".submit-link").bind("click", function() {
      if ($(this).attr("data-nowait") == undefined) {
        $(this).html("Please wait...");
      }
      
      if ($(this).attr("data-confirm") == undefined) {
        // Directly follow form if no confirmation was requested.
        $(this).parent().submit();
        return false;
      }

      if (confirm($(this).attr("data-confirm"))) {
        $(this).parent().submit();
      }

      return false;
    });

    $("a").bind("click", function(){
      // Avoid double handling.
      if ($(this).hasClass("submit-link")) {
        return false;
      }

      if ($(this).attr("data-confirm") == undefined) {
        return true;
      } else {
        if (confirm($(this).attr("data-confirm"))) {
          return true;
        } else {
          return false;
        }
      }
    });

    // Show full message in sidebar.
    bindMessageSidebarClicks();

    // User role settings in new user form.
    $("#user_role").bind("change", function() {
        if ($(this).val() == "reader") {
          $(".users-streams").show();
        } else {
          $(".users-streams").hide();
        }
    });

    // Set sidebar to a fixed height to get the scrollbar in lower resolutions.
    $("#sidebar").css("height", $(window).height()-120);

    // Facilities title change.
    $(".facilities-edit-link").bind("click", function() {
      $(this).hide();
      $(this).parent().next().show();
      return false;
    });


    // Additional field quickfilter.
    $("#messages-quickfilter-add-additional").bind("click", function() {
      field = "<dt><input name='filters[additional][keys][]' type='text' class='messages-quickfilter-additional-key' /></dt>"
      field += "<dd><input name='filters[additional][values][]' type='text' class='messages-quickfilter-additional-value' /></dd>"

      $("#messages-quickfilter-fields").append(field);
      reloadAutoCompleteAdditionalFields(); // Defined in quickfilter partial.
      return false;
    })

    // Health: Show all indices.
    $("#health-show-all-indices").bind('click', function() {
      $(this).hide();
      $("#health-indices-more").slideDown();
      return false;
    });

    // Stream output chooser.
    $('#stream-outputs-chooser').bind('change', function() {
      $(".stream-output-fields").hide();
      $("#stream-output-fields-" + $(this).val().replace(/\./g, "_")).show();
    });

    // Stream output show edit form.
    $('.stream-output-edit-link').bind('click', function() {
      $("#stream-output-edit-" + $(this).attr("data-output-id")).toggle();
      return false;
    });

    // Stream rule show edit form.
    $('.stream-rule-edit-link').bind('click', function() {
      $("#stream-rule-edit-" + $(this).attr("data-rule-id")).toggle();
      return false;
    });

    // Key bindings.
    //standardMapKeyOptions = { overlayClose:true }
    //$.mapKey("s", function() { $("#modal-stream-chooser").modal(standardMapKeyOptions); });
    //$.mapKey("h", function() { $("#modal-host-chooser").modal(standardMapKeyOptions); });
  
    var count;

    setInterval(function(){
      // Update current throughput every 5 seconds
      $.post("/health/currentthroughput", function(json) {
        count = $(".health-throughput-current");
        count.html(parseInt(json.count));
        count.fadeOut(200, function() {
          count.fadeIn(200);
        });
      }, "json");
    }, 5000);
});

function buildHostCssId(id) {
  return "visuals-spread-hosts-" + id.replace(/=/g, '');
};

function bindMessageSidebarClicks() {
  $(".message-row").bind("click", function() {
    $("#gln").show();

    target = relative_url_root + "/messages/" + $(this).attr("id") + "?partial=true";
    
    stream_id = $("#stream_id").val();
    if (stream_id != undefined) {
      target += "&stream_id=" + stream_id;
    }

    $.post(target, function(data) {
      $("#sidebar-inner").html(data);

      // Show sidebar if hidden.
      if (!$("#main-right").is(":visible")) {
        $("#main-left").animate({ width: '65%' }, 700, function() {
          // Show sidebar when main body is completely squeezed.
          $("#main-right").show();
        });
      }

      $("#gln").hide();
    });
  });
};

// srsly, javascript... - http://stackoverflow.com/questions/1219860/javascript-jquery-html-encoding
function htmlEncode(v) {
  return $('<div/>').text(v).html();
};

function notify(what) {
  $.gritter.add({
    title: "Notification",
    text: what
  })
};
