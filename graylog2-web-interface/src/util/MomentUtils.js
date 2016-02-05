import moment from 'moment';

const MomentUtils = {
  startOfResolution(dateTime, resolution) {
    let effectiveResolution = resolution;
    if (resolution === 'week') {
      effectiveResolution = 'isoWeek'; // Weeks should start on Monday :)
    }

    return moment(dateTime).startOf(effectiveResolution);
  },
};

export default MomentUtils;
