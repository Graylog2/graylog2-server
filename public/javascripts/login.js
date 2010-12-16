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

    $("#submit").bind("click", function() {
      // No mutiple submit.
      if ($(this).hasClass("submit-disabled")) {
        return false;
      }

      vals = [
        "Releasing gorillas",
        "Mounting party hats",
        "Enraging gorillas",
        "Preparing gorilla party"
      ]

      $(this).html(vals[Math.floor(Math.random() * vals.length)] + "...");
      $(this).addClass("submit-disabled");

      $("#loginform").submit();
      return false;
    });
});
