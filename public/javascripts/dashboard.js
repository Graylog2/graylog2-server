$(document).ready(function(){
    $('#problems').anythingSlider({
            easing: "swing",                // Anything other than "linear" or "swing" requires the easing plugin
            autoPlay: true,                 // This turns off the entire FUNCTIONALY, not just if it starts running or not
            startStopped: false,            // If autoPlay is on, this can force it to start stopped
            delay: 3000,                    // How long between slide transitions in AutoPlay mode
            animationTime: 600,             // How long the slide transition takes
            hashTags: true,                 // Should links change the hashtag in the URL?
            buildNavigation: false,          // If true, builds and list of anchor links to link to each slide
            pauseOnHover: true,             // If true, and autoPlay is enabled, the show will pause on hover
            startText: "",             // Start text
            stopText: "",               // Stop text
            navigationFormatter: null       // Details at the top of the file on this use (advanced use)
    });
});
