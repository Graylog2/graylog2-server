$(document).ready(function() {

    /*
     * Check if a dropdown overflows the screen size.
     * parent: element used to calculate the position where the element will appear on the screen.
     * element: element to be drawn.
     * useParentWidth: (boolean) true if element will appear on the right size of parent.
     */
    function elementOverflowScreenWidth(parent, element, useParentWidth) {
        if (useParentWidth) {
            return ((window.screen.width - $(parent).offset().left) <= ($(parent).outerWidth() + $(element).outerWidth()));
        } else {
            return ((window.screen.width - $(parent).offset().left) <= $(element).outerWidth());
        }
    }

    $(".dropdown-toggle").on("click", function() {
        var menu = $(this).siblings("ul.dropdown-menu").first();

        // Check if the menu overflows
        if (elementOverflowScreenWidth(this, menu, false)) {
            if (!menu.hasClass("right-menu")) {
                menu.addClass("right-menu");
            }
        } else {
            if (menu.hasClass("right-menu")) {
                menu.removeClass("right-menu");
            }
        }
    });

    $(".dropdown-submenu").on("hover", function() {
        var menu = $("ul.dropdown-menu", this).first();

        if (elementOverflowScreenWidth(this, menu, true)) {
            if (!$(this).hasClass("left-submenu")) {
                $(this).addClass("left-submenu");
            }
        } else {
            if ($(this).hasClass("left-submenu")) {
                $(this).removeClass("left-submenu");
            }
        }
    });
});
