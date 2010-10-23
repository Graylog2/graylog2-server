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
        }
        field.removeAttr("disabled");
        field.show();
    });

    // Stream Quick chooser
    $('#favoritestreamchooser_id').bind('change', function() {
       window.location = "/streams/show/" + parseInt(this.value);
    });

    // Show stream statistics
    $('#streams-show-statistics').bind('click', function() {
        $('#blocks-statistics').show();
        $('#streams-show-statistics').hide();
        $.post("/streams/get_hosts_statistic/" + parseInt($('#streamid').html()), function(data) {
                $('#blocks-statistics').html(data);
        });
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

    // Show similar messages in GELF show view.
    $('#messages-show-similar-messages-link').bind('click', function() {
        $('#messages-show-similar-messages-link').hide();
        $('.loading').show();

        $.post("/messages/getsimilarmessages", {id: $('#message-id').html()}, function(data) {
            $('#messages-show-similar-messages').html(data);
        });
        return false;
    });

    $('#messages-show-message-full').css('width', parseInt($('#content').css('width'))-15);
    $('#messages-show-message-full').css('height', parseInt($('#messages-show-message-full').css('height'))+10);
});
