$(document).ready(function() {

    $(".open-analyze-field").on("click", function(e) {
        e.preventDefault();

        $(".analyze-field", $(this).parent()).toggle();
        $(this).toggleClass("open-analyze-field-active");

        if($(this).hasClass("fa-caret-right")) {
            $(this).removeClass("fa-caret-right");
            $(this).addClass("fa-caret-down");
        } else {
            $(this).removeClass("fa-caret-down");
            $(this).addClass("fa-caret-right");
        }
    });

});