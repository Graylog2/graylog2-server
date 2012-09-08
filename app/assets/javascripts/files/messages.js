$(document).ready(function() {

    var messagesHelper = function(elem) {

      if ($(elem).parent().hasClass("messages-full-message")) {
        return confirm("Really follow this link?");
      }

      if ($(elem).attr("class") == "messages-link") { 
        if ($(elem).attr("data-modal")) {
          $("#messages-show-terms-modal").modal({ overlayClose: true });
        }

        if (!$(elem).attr("data-confirm")) {
          return true;
        } else {
          if (confirm($(elem).attr("data-confirm"))) {
            return true;
          } else {
            return false;
          }
        }
      }
    };

    $("#sidebar-inner").delegate("a", "click", function(e) {
      messagesHelper(this);
    });

    $("#content-inner").delegate("a", "click", function(e) {
      messagesHelper(this);
    });
});
