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

function calculateValueFontSize(nowCount) {
    var fontSize;
    var numberOfDigits = nowCount.toFixed().length;

    if (numberOfDigits < 7) {
        fontSize = "70px";
    } else {
        switch (numberOfDigits) {
            case 7:
                fontSize = "60px";
                break;
            case 8:
                fontSize = "50px";
                break;
            case 9:
            case 10:
                fontSize = "40px";
                break;
            case 11:
            case 12:
                fontSize = "35px";
                break;
            default:
                fontSize = "25px";
        }
    }

    return fontSize;
}

function displayTrendIndicator(widget, nowCount, previousCount) {
    var green = "#2AAB2A";
    var red = "#BD362F";
    var grey = "#EBEBEB";
    var lowerIsBetter = Boolean($(".trend-icons", widget).data("lower-is-better"));
    var lowerColor = lowerIsBetter ? green : red;
    var higherColor = lowerIsBetter ? red : green;
    var higherIndicator = $(".trend-higher", widget);
    var lowerIndicator = $(".trend-lower", widget);

    higherIndicator.children().css("color", grey);
    lowerIndicator.children().css("color", grey);

    if (nowCount > previousCount) {
        higherIndicator.children().css("color", higherColor);
    } else if (previousCount > nowCount) {
        lowerIndicator.children().css("color", lowerColor);
    }
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
    var valueElement = $(".value", widget);
    valueElement.text(numeral(nowCount).format());

    var fontSize = calculateValueFontSize(nowCount);
    valueElement.css("font-size", fontSize);

    if (previousCount !== null) {
        displayTrendIndicator(widget, nowCount, previousCount);
    }
}