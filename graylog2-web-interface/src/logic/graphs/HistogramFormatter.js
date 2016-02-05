import moment from 'moment';
import MomentUtils from 'util/MomentUtils';

const HistogramFormatter = {
  _firstDataPointTimestamp(dataPoints, queryFrom, isSearchAll) {
    if (isSearchAll) {
      return moment.utc(Number(Object.keys(dataPoints).sort()[0]) * 1000);
    }

    return moment.utc(queryFrom);
  },

  format(dataPoints, queryTimeRange, resolution, maxDataPoints, isSearchAll) {
    const formattedPoints = [];

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
            y: result,
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
