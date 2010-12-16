$(document).ready(function(){
    $(".login-credentials").bind("focusin", function() {
      if ($(this).hasClass("initial")) {
        $(this).removeClass("initial");
        $(this).val("");
      }
    });

    $(".login-credentials").bind("focusout", function() {
      if ($(this).val() == "") {
        $(this).addClass("initial");
        $(this).val($(this).attr("data-stdtext"));
      }
    });
});
