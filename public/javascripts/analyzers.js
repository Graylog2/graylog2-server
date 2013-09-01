$(document).ready(function() {

    $(".open-analyze-field").on("click", function() {
        $(".analyze-field", $(this).parent()).toggle();
        $(this).toggleClass("open-analyze-field-active");
    });

    $(".analyze-field .generate-overview").on("click", function() {
        var container = $(this).parent();
        $(this).attr("disabled", "disabled");

        showOverview($(this).attr("data-field"), container);
    })

    function showOverview(field, container) {
        var overview = $(".overview", container);
        overview.show();

        showSpinner(overview);

        /*$.ajax({
            url: '/a/messages/' + index + '/' + messageId,
            success: function(data) {
                // do shit
            },
            error: function() {
                showError("Could not load term statistics.");
            },
            complete: function() {
                hideSpinner(overview);
            }
        });*/
    }

    function showSpinner(container) {
        $(".analyzer-content", container).hide();
        $(".spinner", container).show();
    }

    function hideSpinner(container) {
        $(".spinner", container).hide();
        $(".analyzer-content", container).show();
    }

});