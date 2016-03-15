import moment from 'moment';
import {} from 'moment-duration-format';

const ISODurationUtils = {
  isValidDuration(duration, validator) {
    return validator(moment.duration(duration).asMilliseconds(), duration);
  },

  durationStyle(duration, validator, errorClass) {
    let className = errorClass;
    if (!className) {
      className = 'error';
    }
    return this.isValidDuration(duration, validator) ? null : className;
  },

  formatDuration(duration, validator, errorText) {
    let text = errorText;
    if (!text) {
      text = 'error';
    }
    return this.isValidDuration(duration, validator) ? moment.duration(duration).format() : text;
  },

  humanizeDuration(duration, validator, errorText) {
    let text = errorText;
    if (!text) {
      text = 'error';
    }
    return this.isValidDuration(duration, validator) ? moment.duration(duration).humanize() : text;
  },
};

export default ISODurationUtils;
