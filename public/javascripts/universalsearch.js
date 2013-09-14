$(document).ready(function() {

    $(".timerange-chooser .dropdown-menu a").on("click", function() {
        $(".timerange-selector").hide();
        $("#universalsearch-rangetype").val($(this).attr("data-selector-name"));
        $(".timerange-selector input,select").attr("disabled", "disabled");

        var selected = $("." + $(this).attr("data-selector-name"), $(".universalsearch-form"));
        selected.show();
        $("input,select", selected).removeAttr("disabled");

        $("a", $(this).parent().parent()).removeClass("selected");
        $(this).addClass("selected");
    });

    $("#universalsearch .timerange-selector-container .absolute .date-select").datepicker({
        format: "yyyy-mm-dd"
    }).on("changeDate", function(ev) {
        $(this).val($(this).val() + " 00-00-00");
    });

    $("#universalsearch .timerange-selector-container .absolute .set-to-now").on("click", function() {
        var input = $("input", $(this).parent());

        var date = new Date();
        input.val(dateTimeFormatted(date));
    });

    function dateTimeFormatted(date) {
        var day = ('0' + date.getDate()).slice(-2); // wtf javascript. this returns the day.
        var month = ('0' + (date.getMonth() + 1)).slice(-2);
        var year = date.getFullYear();

        var hour = ('0' + date.getHours()).slice(-2);
        var minute = ('0' + date.getMinutes()).slice(-2);
        var second = ('0' + date.getSeconds()).slice(-2);

        return year + "-" + month + "-" + day + " " + hour + "-" + minute + "-" + second;
    }

});