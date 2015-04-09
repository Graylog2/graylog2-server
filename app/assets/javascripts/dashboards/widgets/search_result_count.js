function configureDialog_search_result_count(callback, element, description) {
    var params = originalUniversalSearchSettings();
    var supportsTrending = params.rangeType === 'relative';
    trendDialogConfiguration.openModal(callback, supportsTrending, description || "messages");
}

function copyInto(config, params) {
    params.description = config.description;
    params.trend = config.trend;
    params.lowerIsBetter = config.lowerIsBetter;
}

function addWidget_search_result_count(dashboardId, config, eventElem) {
    var params = originalUniversalSearchSettings();
    params.widgetType = "SEARCH_RESULT_COUNT";
    copyInto(config, params);
    addWidget(dashboardId, undefined, params);
}