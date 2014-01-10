$(document).ready(function() {

    $(".add-alert").on("click", function() {
        $(".alert-type-form").hide();
        $("#" + $(".add-alert-type").val()).show();
    });

    $(".alert-type-form").on("submit", function(e) {
        return validate("#" + $(this).attr("id"));
    });

});