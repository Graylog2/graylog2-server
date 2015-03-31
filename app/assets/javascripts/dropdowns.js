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

    /*
     * Check if a dropdown overflows a scrollable container.
     * container: div with overflow:hidden.
     * element: element to be drawn.
     * coordinate: ("top"|"bottom") specifies if it should check overflow at the top or bottom of the container.
     */
    function elementOverflowParentHeight(container, element, coordinate) {
        if (coordinate == "top") {
            var containerTop = container.offset().top;
            var elementTop = element.offset().top;
            return elementTop < containerTop;
        } else if (coordinate == "bottom") {
            var containerBottom = container.offset().top + container.outerHeight();
            var elementBottom = element.offset().top + element.outerHeight();
            return elementBottom > containerBottom;
        } else {
            throw new Error("Undefined coordinate '" + coordinate + "', must be 'top' or 'bottom'");
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

    $(document).on("click", ".message-field-dropdown .key", function() {
        var dropdownContainer = $(this).parent();
        var menu = $(this).siblings("ul.dropdown-menu").first();
        var container = $("#sidebar-replacement .nano .content");

        dropdownContainer.removeClass("dropup");

        if (elementOverflowParentHeight(container, menu, "bottom")) {
            dropdownContainer.addClass("dropup");
        }
    });

    $(document).on("hover", ".message-field-dropdown .dropdown-submenu", function() {
        var menu = $(this).children("ul.dropdown-menu").first();
        var container = $("#sidebar-replacement .nano .content");

        // By default, let submenu go down
        $(this).removeClass("up-submenu");
        $(this).addClass("down-submenu");

        if (elementOverflowParentHeight(container, menu, "bottom")) {
            // Move submenu up
            $(this).removeClass("down-submenu");
            $(this).addClass("up-submenu");

            if (elementOverflowParentHeight(container, menu, "top")) {
                $(this).removeClass("up-submenu");
                $(this).addClass("down-submenu");
            }
        }
    });
});
