$(document).ready(function(){
    // Show notifications with gritter if present.
    if ($('#notification-notice').html().length !== 0) {
        title = $('#notification-notice strong').html();
        text = $('#notification-notice span').html();

        if (title == null || title.length == 0) {title = "Notice";}
        if (text == null || text.length == 0) {
            text = $('#notification-notice').html();
            
            // Fallback if we still have nothing at all.
            if (text == null || text.length == 0) {
                text = "Success."
            }
        }

        $.gritter.add({
            title: title,
            text: text,
            image: '/images/icons/okay.png'
        });
    }

    if ($('#notification-error').html().length !== 0) {
        title = $('#notification-error strong').html();
        text = $('#notification-error span').html();

        if (title == null || title.length == 0) {title = "Notice";}
        if (text == null || text.length == 0) {
            text = $('#notification-error').html();

            // Fallback if we still have nothing at all.
            if (text == null || text.length == 0) {
                text = "Success.";
            }
        }

        $.gritter.add({
            title: '<span class="gritter-error-title">' + title + '</span>',
            text: text,
            image: '/images/icons/error.png'
        });
    }

    // Stream rule form.
    $('#streamrule_rule_type').bind('change', function() {
        $('.stream-value-field').hide();
        $('.stream-value-field').attr("disabled", true);
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
                break;
        }
        field.removeAttr("disabled");
        field.show();
    });

    // Stream Quick chooser
    $('#favoritestreamchooser_id').bind('change', function() {
       window.location = "/streams/show/" + parseInt(this.value);
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

    // "more" link in message tables
    $('.messages-more').bind('click', function() {
        var message_id = this.id;
        $('#message-' + message_id).html('<img src="/images/loading-small.gif" alt="loading" style="position: relative; top: 2px;"/>');
        $.post("/messages/getcompletemessage", {id: message_id}, function(data) {
            $('#message-' + message_id).html(data);
        });
        return false;
    });

    // Full message view resizing.
    $('#messages-show-message-full').css('width', parseInt($('#content').css('width'))-15);
    $('#messages-show-message-full').css('height', parseInt($('#messages-show-message-full').css('height'))+10);

    // Visuals: Message spread permalink
    $('#visuals-spread-hosts-permalink-link').bind('click', function() {
      $('#visuals-spread-hosts-permalink-link').hide();
      $('#visuals-spread-hosts-permalink-content').show();
      return false;
    });

    $('#analytics-new-messages-update-hours').numeric();
    // Visuals: Update of new messages graph.
    $('#analytics-new-messages-update-submit').bind('click', function() {
      i = $('#analytics-new-messages-update-hours');
      v = parseInt(i.val());
      
      if (v <= 0) {
        return false;
      }

      // Show loading message.
      $("#analytics-new-messages-update-loading").show();
      
      // Update graph.
      $.post($(this).attr("data-updateurl") + v, function(data) {
        json = eval('(' + data + ')');
      
        // Plot is defined inline. (I suck at JavaScript)
        plot(json.data);
      
        // Update title.
        $('#analytics-new-messages-hours').html(v);
        
        // Hide loading message.
        $("#analytics-new-messages-update-loading").hide();
      });

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
    
    // Stream alerts: Inputs only numeric.
    $('#streams-alerts-limit').numeric();
    $('#streams-alerts-timespan').numeric();

    // Stream descriptions.
    $(".stream-description-change").bind("click", function() {
      $("#streams-description-text").hide();
      $("#streams-set-description").show();
    });

    // Awesome submit links
    $(".awesome-submit-link").bind("click", function() {
      if ($(this).attr("data-nowait") == undefined) {
        $(this).html("Please wait...");
      }
      $(this).parent().submit();
      return false;
    });

    // Show full message in sidebar.
    $(".message-row").bind("click", function() {
      $("#gln").show();

      $.post("/messages/show/" + $(this).attr("id") + "?partial=true", function(data) {
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

});

function buildHostCssId(id) {
  return "visuals-spread-hosts-" + id.replace(/=/g, '');
}
