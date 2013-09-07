$(document).ready(function() {

    $(".timerange-chooser .dropdown-menu a").on("click", function() {
        $(".timerange-selector").hide();
        $(".timerange-selector input,select").attr("disabled", "disabled");

        var selected = $("." + $(this).attr("data-selector-name"), $(".universalsearch-form"));
        selected.show();
        $("input,select", selected).removeAttr("disabled");
    });

    $("#universalsearch .timerange-selector-container .absolute .date-select").datepicker({
        format: "yyyy-mm-dd"
    }).on("changeDate", function(ev) {
        var d = new Date();
        var hours = ('0' + d.getHours()).slice(-2);
        var minutes = ('0' + d.getMinutes()).slice(-2);

        $(this).val($(this).val() + " " + hours + "-" + minutes + "-00.000");
    });

});