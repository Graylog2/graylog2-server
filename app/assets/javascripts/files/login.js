
$(document).ready(function(){

    var vals = [
          "Releasing gorillas",
          "Mounting party hats",
          "Enraging gorillas",
          "Preparing gorilla party"
        ],
        $loginCredentials = $(".login-credentials"),
        $loginForm = $("#loginform"),
        $submitButton = $("#submit"),
        code,
        button;

    $loginCredentials.bind("focusin", function(e) {
      e && e.preventDefault();

      if ($(this).hasClass("initial")) {
        $(this).removeClass("initial");
        $(this).val("");
      }
    });

    $loginCredentials.bind("focusout", function(e) {
      e && e.preventDefault();

      if (!$(this).val()) {
        $(this).addClass("initial");
        $(this).val($(this).attr("data-stdtext"));
      }
    });

    $loginCredentials.bind("keypress", function(e) {
      code = (e.keyCode ? e.keyCode : e.which);
      if (code && code === 13) {
        $loginForm.submit();
      }
    });

    $submitButton.bind("click", function(e) {
      e && e.preventDefault();

      $loginForm.submit();
    });

    $loginForm.bind("submit", function(e) {
      // No mutiple submit.
      if ($submitButton.hasClass("submit-disabled")) {
        return;
      }
      $submitButton.html(vals[Math.floor(Math.random() * vals.length)] + "...");
      $submitButton.addClass("submit-disabled");

      $(this).submit();
    });
});
