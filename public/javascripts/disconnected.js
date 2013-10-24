$(document).ready(function () {

    // make the config node box clickable
    $(".discovered-node-link").on("click", function () {
        var nodeId = $(this).attr("data-discovered-node");
        var nodeElem = $("div[data-node-id=\"" + nodeId + "\"]");
        nodeElem.effect("pulsate");
    });

    $(".via-node-headline").on("click", function () {
        var address = $(this).attr("data-discovered-via");
        var nodeElem = $("div[data-config-node-address=\"" + address + "\"]");
        nodeElem.effect("pulsate");
    });

});

