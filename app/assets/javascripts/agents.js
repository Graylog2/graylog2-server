$(document).ready(function() {
    $(".toggleInactive").toggle(function() {
        $(this).text("Hide inactive");
        $("tr.inactive").show();
    }, function() {
        $(this).text("Show inactive");
        $("tr.inactive").hide();
    })
});