import DateTime from 'logic/datetimes/DateTime';
import moment from 'moment';

export default {
  dateTimeFormats: [
    { formatString: ".SSS",   condition: function(d) { return d.milliseconds() !== 0; }},
    { formatString: ":s",     condition: function(d) { return d.seconds() !== 0; }},
    { formatString: "HH:mm",  condition: function(d) { return d.minutes() !== 0; }},
    { formatString: "HH:mm",  condition: function(d) { return d.hours() !== 0; }},
    { formatString: "ddd DD", condition: function(d) { return d.isoWeekday() !== 1 && d.date() !== 1; }},
    { formatString: "MMM DD", condition: function(d) { return d.date() !== 1; }},
    { formatString: "MMM",    condition: function(d) { return d.month() !== 0; }},
    { formatString: "YYYY",   condition: function() { return true; }}
  ],

  intervalResolutions: [
    { interval: 'year',    unit: 'year',    step: 1,  condition: function(duration) { return duration.years() > 1 }},
    { interval: 'month',   unit: 'month',   step: 1,  condition: function(duration) { return duration.years() === 1 || duration.months() > 1 }},
    { interval: 'day',     unit: 'date',    step: 2,  condition: function(duration) { return duration.months() === 1 || duration.days() > 10 }},
    { interval: 'day',     unit: 'date',    step: 1,  condition: function(duration) { return duration.days() > 3 }},
    { interval: 'hour',    unit: 'hour',    step: 12, condition: function(duration) { return duration.days() > 1 }},
    { interval: 'hour',    unit: 'hour',    step: 3,  condition: function(duration) { return duration.days() === 1 }},
    { interval: 'hour',    unit: 'hour',    step: 1,  condition: function(duration) { return duration.hours() > 1 }},
    { interval: 'minute',  unit: 'minute',  step: 10, condition: function(duration) { return duration.hours() === 1 || duration.minutes() > 30 }},
    { interval: 'minute',  unit: 'minute',  step: 5,  condition: function(duration) { return duration.minutes() > 15 }},
    { interval: 'minute',  unit: 'minute',  step: 1,  condition: function() { return true }}
  ],

  customDateTimeFormat(tzOffset) {
    if (typeof tzOffset === 'undefined') {
      tzOffset = null;
    }
    return (date) => {
      var momentDate;
      var formattedDate;

      if (tzOffset === null) {
        momentDate = new DateTime(date).toMoment();
      } else {
        momentDate = moment(date);
        momentDate.utcOffset(tzOffset);
      }

      this.dateTimeFormats.some(function (format) {
        if (format.condition(momentDate) === true) {
          formattedDate = momentDate.format(format.formatString);
          return true;
        }
      });

      return formattedDate;
    }
  },

  customTickInterval(tzOffset) {
    if (typeof tzOffset === 'undefined') {
      tzOffset = null;
    }
    return (initDateTime, endDateTime, step) => {
      var ticks = [];
      var runningMoment;
      var endMoment;

      if (tzOffset === null) {
        runningMoment = new DateTime(initDateTime).toMoment();
        endMoment = new DateTime(endDateTime).toMoment();
      } else {
        runningMoment = moment(initDateTime);
        endMoment = moment(endDateTime);
        runningMoment.utcOffset(tzOffset);
        endMoment.utcOffset(tzOffset);
      }

      if (!runningMoment.isValid() || !endMoment.isValid()) {
        return ticks;
      }

      var interval;
      var unit;
      var duration = moment.duration(endMoment.valueOf() - runningMoment.valueOf());
      this.intervalResolutions.some(function(resolution) {
        if (resolution.condition(duration)) {
          interval = resolution.interval;
          unit = resolution.unit;
          step = resolution.step;
          return true;
        }
      });

      runningMoment.add(1, interval).startOf(interval);

      if (step > 1) {
        while (runningMoment.isBefore(endMoment)) {
          if ((runningMoment.get(unit) % step) === 0) {
            ticks.push(new Date(runningMoment.valueOf()));
          }
          runningMoment.add(1, interval);
        }
      } else {
        while (runningMoment.isBefore(endMoment)) {
          ticks.push(new Date(runningMoment.valueOf()));
          runningMoment.add(1, interval);
        }
      }

      return ticks;
    }
  },
}

