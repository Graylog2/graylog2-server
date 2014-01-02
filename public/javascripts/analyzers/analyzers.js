$(document).ready(function() {

    $(".open-analyze-field").on("click", function(e) {
        e.preventDefault();

        $(".analyze-field", $(this).parent()).toggle();
        $(this).toggleClass("open-analyze-field-active");
    });

});