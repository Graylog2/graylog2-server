import moment from 'moment';
import MomentUtils from 'util/MomentUtils';
import NumberUtils from 'util/NumberUtils';

const DEFAULT_MAX_DATA_POINTS = 4000;

const HistogramFormatter = {
  _firstDataPointTimestamp(dataPoints, queryFrom, isSearchAll) {
    if (isSearchAll) {
      return moment.utc(Number(Object.keys(dataPoints).sort()[0]) * 1000);
    }

    return moment.utc(queryFrom);
  },

  format(dataPoints, queryTimeRange, resolution, maxDataPoints, isSearchAll) {
    const formattedPoints = [];
    const maxDataPoints = (screenSize && screenSize > 0 ? screenSize : DEFAULT_MAX_DATA_POINTS);

    if (typeof dataPoints === 'object' && !Array.isArray(dataPoints)) {
      const from = this._firstDataPointTimestamp(dataPoints, queryTimeRange.from, isSearchAll);
      const to = moment.utc(queryTimeRange.to);

      let tempTime = moment(from);
      const step = moment.duration(1, resolution);

      const totalDataPoints = ((to.valueOf() - from.valueOf()) / step.as('milliseconds')).toFixed();
      const factor = (totalDataPoints > maxDataPoints ? (totalDataPoints / maxDataPoints).toFixed() : 1);

      let index = 0;
      // Position the temporary time to the beginning of the time span we want to graph and iterate through it
      tempTime = MomentUtils.startOfResolution(tempTime, resolution);
      while (tempTime.isBefore(to) || tempTime.isSame(to)) {
        if (index % factor === 0) {
          const timestamp = String(tempTime.unix());
          const value = dataPoints[timestamp];
          const result = ((value === null || value === undefined) ? 0 : value);
          formattedPoints.push({
            x: tempTime.valueOf(),
            y: NumberUtils.normalizeGraphNumber(result),
          });
        }
        index += 1;
        tempTime.add(step);
      }
    }

    return formattedPoints;
  },
};

export default HistogramFormatter;
