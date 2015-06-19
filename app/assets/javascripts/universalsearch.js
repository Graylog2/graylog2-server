function activateTimerangeChooserV2(rangeType, rangeParams) {
    "use strict";
    $(document).trigger('change-timerange.graylog.search', {rangeType: rangeType, rangeParams: rangeParams});
}

function submitSearch() {
    "use strict";
    $(document).trigger('execute.graylog.search');
}
