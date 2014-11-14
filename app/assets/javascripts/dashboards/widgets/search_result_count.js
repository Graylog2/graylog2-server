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
    $(".value", widget).text(numeral(data.result).format());
}