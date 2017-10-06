import PropTypes from 'prop-types';

const TimeRangeType = PropTypes.oneOf(['relative', 'absolute', 'keyword']);

export default Object.assign({ TimeRangeType: TimeRangeType }, PropTypes);
