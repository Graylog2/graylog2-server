$(document).ready(function(){
    // Show notifications with gritter if present.
    if ($('#notification-notice').html().length !== 0) {
        title = $('#notification-notice strong').html();
        text = $('#notification-notice span').html();

        if (title == null || title.length == 0) { title = "Notice"; }
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

        if (title == null || title.length == 0) { title = "Notice"; }
        if (text == null || text.length == 0) {
            text = $('#notification-error').html();

            // Fallback if we still have nothing at all.
            if (text == null || text.length == 0) {
                text = "Success."
            }
        }

        $.gritter.add({
            title: '<span class="gritter-error-title">' + title + '</span>',
            text: text,
            image: '/images/icons/error.png'
        });
    }

});