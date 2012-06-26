$(document).ready(function(){

  var $backToTopButton = $("#back-to-top"),
      scrolled = false,
      interval;

  $backToTopButton.bind("click", function(e) {
    e && e.preventDefault();
    $("body,html").animate({scrollTop:0}, 500);
  });

  $(window).bind("scroll", function(e) {
    scrolled = true;
  });

  interval = setInterval(function() {
    if (scrolled) {
      scrolled = false;
      if($(this).scrollTop() > 150) {
        $backToTopButton.fadeIn();
      } else {
        $backToTopButton.fadeOut();
      }
    }
  }, 300);
});
