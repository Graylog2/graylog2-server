function configureDialog_search_result_count(callback) {
    trendDialogConfiguration.openModal(callback);
}

function addWidget_search_result_count(dashboardId, config, eventElem) {
    var params = originalUniversalSearchSettings();
    params.widgetType = "SEARCH_RESULT_COUNT";

    // description: "", trend: false, amount: 1, unit: "days"
    params.description = config.description;
    params.trend = config.trend;
    params.amount = config.amount;
    params.unit = config.unit;

    addWidget(dashboardId, undefined, params);
}

function updateWidget_search_result_count(widget, data) {
    var nowCount = null, previousCount = null;
    var result = data.result;
    if (typeof result === 'object') {
        nowCount = result.now;
        previousCount = result.previous;
    } else {
        nowCount = result;
    }
    $(".value", widget).text(numeral(nowCount).format());

    if (previousCount !== null) {
        var green = "#2AAB2A";
        var red = "#BD362F";
        var blue = "#08C";
        var arrowUp = $(".trend-higher", widget);
        var arrowDown = $(".trend-lower", widget);
        if (nowCount > previousCount) {
            arrowUp.show();
            arrowUp.children().css("color", green);
            arrowDown.hide();
        } else if (previousCount > nowCount) {
            arrowUp.hide();
            arrowDown.show();
            arrowDown.children().css("color", red);
        } else {
            arrowUp.show();
            arrowUp.children().css("color", blue);
            arrowDown.show();
            arrowDown.children().css("color", blue);
        }
    }
}