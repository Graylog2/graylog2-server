'use strict';

(function() {
    var dateTimeFormats = [
        { formatString: ".SSS",   condition: function(d) { return d.milliseconds() !== 0; }},
        { formatString: ":s",     condition: function(d) { return d.seconds() !== 0; }},
        { formatString: "HH:mm",  condition: function(d) { return d.minutes() !== 0; }},
        { formatString: "HH:mm",  condition: function(d) { return d.hours() !== 0; }},
        { formatString: "ddd DD", condition: function(d) { return d.isoWeekday() !== 1 && d.date() !== 1; }},
        { formatString: "MMM DD", condition: function(d) { return d.date() !== 1; }},
        { formatString: "MMM",    condition: function(d) { return d.month() !== 0; }},
        { formatString: "YYYY",   condition: function() { return true; }}
    ];

    var intervalResolutions = [
        { interval: 'year',  unit: 'year',  step: 1,  condition: function(duration) { return duration.years() > 1 }},
        { interval: 'month', unit: 'month', step: 1,  condition: function(duration) { return duration.years() === 1 || duration.months() > 1 }},
        { interval: 'day',   unit: 'date',  step: 2,  condition: function(duration) { return duration.months() === 1 || duration.days() > 10 }},
        { interval: 'day',   unit: 'date',  step: 1,  condition: function(duration) { return duration.days() > 3 }},
        { interval: 'hour',  unit: 'hour',  step: 12, condition: function(duration) { return duration.days() > 1 }},
        { interval: 'hour',  unit: 'hour',  step: 3,  condition: function(duration) { return duration.days() == 1 }},
        { interval: 'hour',  unit: 'hour',  step: 1,  condition: function() { return true }}
    ];

    var exports = {
        customDateTimeFormat: function(tzOffset) {
            return function(date) {
                var momentDate = moment.utc(date);
                var formattedDate;

                if (tzOffset !== null) {
                    momentDate.zone(tzOffset);
                }

                dateTimeFormats.some(function (format) {
                    if (format.condition(momentDate) === true) {
                        formattedDate = momentDate.format(format.formatString);
                        return true;
                    }
                });

                return formattedDate;
            }
        },

        customTickInterval: function(tzOffset) {
            return function(initDateTime, endDateTime, step) {
                var ticks = [];
                var tempMoment = moment(initDateTime);
                var endMoment = moment(endDateTime);

                if (!tempMoment.isValid() || !endMoment.isValid()) {
                    return ticks;
                }

                if (tzOffset !== null) {
                    tempMoment.zone(tzOffset);
                    endMoment.zone(tzOffset);
                }

                var interval;
                var unit;
                var duration = moment.duration(endMoment.valueOf() - tempMoment.valueOf());
                intervalResolutions.some(function(resolution) {
                    if (resolution.condition(duration)) {
                        interval = resolution.interval;
                        unit = resolution.unit;
                        step = resolution.step;
                        return true;
                    }
                });

                tempMoment.add(1, interval).startOf(interval);

                if (step > 1) {
                    while (tempMoment.isBefore(endMoment)) {
                        if ((tempMoment.get(unit) % step) === 0) {
                            ticks.push(new Date(tempMoment.valueOf()));
                        }
                        tempMoment.add(1, interval);
                    }
                } else {
                    while (tempMoment.isBefore(endMoment)) {
                        ticks.push(new Date(tempMoment.valueOf()));
                        tempMoment.add(1, interval);
                    }
                }

                return ticks;
            }
        }
    };
    if (typeof module === 'object' && typeof module.exports === 'object') {
        module.exports = exports;
    } else {
        window.graphHelper = exports;
    }
})();
