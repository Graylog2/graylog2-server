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
    $('#messages-show-quickfilter').toggle(function() {
          $('#messages-quickfilter').show();
          $('#messages-show-quickfilter').animate({width: 150}, 100);
        }, function() {
          $('#messages-quickfilter').hide();
          $('#messages-show-quickfilter').animate({width: 125}, 100);
    });

});