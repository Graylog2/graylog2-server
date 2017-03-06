import Rickshaw from 'rickshaw';
import moment from 'moment';
import graphHelper from 'legacy/graphHelper';

Rickshaw.namespace('Rickshaw.Fixtures.Graylog2Time');

const Graylog2Time = function () {
  const self = this;

  this.months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

  this.units = [
    {
      name: 'decade',
      seconds: 86400 * 365.25 * 10,
      formatter(d) { return self.formatDate(d); },
    }, {
      name: 'year',
      seconds: 86400 * 365.25,
      formatter(d) { return self.formatDate(d); },
    }, {
      name: 'month',
      seconds: 86400 * 30.5,
      formatter(d) { return self.formatDate(d); },
    }, {
      name: 'week',
      seconds: 86400 * 7,
      formatter(d) { return self.formatDate(d); },
    }, {
      name: 'day',
      seconds: 86400,
      formatter(d) { return self.formatDate(d); },
    }, {
      name: '3 hours',
      seconds: 3600 * 3,
      formatter(d) { return self.formatDateTime(d); },
    }, {
      name: 'hour',
      seconds: 3600,
      formatter(d) { return self.formatDateTime(d); },
    }, {
      name: '15 minutes',
      seconds: 60 * 15,
      formatter(d) { return self.formatDateTime(d); },
    }, {
      name: '5 minutes',
      seconds: 60 * 5,
      formatter(d) { return self.formatDateTime(d); },
    }, {
      name: 'minute',
      seconds: 60,
      formatter(d) { return self.formatDateTime(d); },
    }, {
      name: '15 second',
      seconds: 15,
      formatter(d) { return self.formatDateTime(d); },
    }, {
      name: 'second',
      seconds: 1,
      formatter(d) { return self.formatDateTime(d); },
    }, {
      name: 'millisecond',
      seconds: 1 / 1000,
      formatter(d) { return self.formatDateTime(d); },
    },
  ];

  this.unit = function (unitName) {
    return this.units.filter((unit) => { return unitName == unit.name; }).shift();
  };

  this.formatDateTime = graphHelper.customDateTimeFormat();

	// Data in histograms is calculated using UTC. When the histogram resolution is day or lower,
	// we can't extrapolate the data to the user's local time, so we use UTC instead.
  this.formatDate = graphHelper.customDateTimeFormat(0);

  this.ceil = function (time, unit) {
    let date,
      floor,
      year;

    if (unit.name == 'week') {
      const momentDate = moment.utc(time * 1000);
      momentDate.startOf('isoWeek');

      if (momentDate.unix() == time) return time;

      momentDate.add(1, 'week');
      return momentDate.unix();
    }

    if (unit.name == 'month') {
      date = new Date(time * 1000);

      floor = Date.UTC(date.getUTCFullYear(), date.getUTCMonth()) / 1000;
      if (floor == time) return time;

      year = date.getUTCFullYear();
      let month = date.getUTCMonth();

      if (month == 11) {
        month = 0;
        year += 1;
      } else {
        month += 1;
      }

      return Date.UTC(year, month) / 1000;
    }

    if (unit.name == 'year') {
      date = new Date(time * 1000);

      floor = Date.UTC(date.getUTCFullYear(), 0) / 1000;
      if (floor == time) return time;

      year = date.getUTCFullYear() + 1;

      return Date.UTC(year, 0) / 1000;
    }

    return Math.ceil(time / unit.seconds) * unit.seconds;
  };
};

Rickshaw.Fixtures.Graylog2Time = Graylog2Time;

export default Graylog2Time;
