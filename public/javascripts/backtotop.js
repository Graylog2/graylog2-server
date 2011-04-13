$(document).ready(function(){
    $('#back-to-top').bind("click", function() {
      $('body,html').animate({scrollTop:0},500);
    });
});

$(window).scroll(function() {
  if($(this).scrollTop() > 150) {
    $('#back-to-top').fadeIn();
  } else {
    $('#back-to-top').fadeOut();
  }
});
