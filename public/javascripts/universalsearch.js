$(document).ready(function() {

    $(".timerange-chooser .dropdown-menu a").on("click", function() {
        $(".timerange-selector").hide();
        $(".timerange-selector input,select").attr("disabled", "disabled");

        var selected = $("." + $(this).attr("data-selector-name"), $(".universalsearch-form"));
        selected.show();
        $("input,select", selected).removeAttr("disabled");
    });

});