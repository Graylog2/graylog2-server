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

  /**
   * Formats the histogram to add empty data where there were no results, normalizes its values to
   * avoid errors in the underlying graphing library, and samples the data, so we don't draw more
   * points that we can display.
   *
   * @param {object} dataPoints: Histogram data as returned from the server {unix_time: value, ...}
   * @param {object} queryTimeRange: Time ranges of the query that generated the histogram
   * @param {string} resolution: Histogram resolution
   * @param {number} screenSize: Screen width to calculate the maximum number of data points returned
   * @param {boolean} isSearchAll: Indicates if the histogram was made for a search in all messages or not
   * @param {string} valueKey: Histogram contains objects with different values, this indicates the key
   *                           to access the value to plot
   * @param {boolean} legacy: Flag to indicate if the x-axis should contain timestamps in ms or seconds.
   *                          This was added as Rickshaw only supports the latter format
   *
   */
  format(dataPoints, queryTimeRange, resolution, screenSize, isSearchAll, valueKey, legacy = false) {
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

          let result = 0;
          // Get the actual value if the result for that time is an object. If there is no result, we'll use 0
          if (value !== null && value !== undefined) {
            if (typeof value === 'object') {
              if (value.hasOwnProperty(valueKey)) {
                result = value[valueKey];
              }
            } else {
              result = value;
            }
          }

          formattedPoints.push({
            x: (legacy ? tempTime.unix() : tempTime.valueOf()),
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
