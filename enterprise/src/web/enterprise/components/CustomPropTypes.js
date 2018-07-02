import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';
import FieldTypeMapping from '../logic/fieldtypes/FieldTypeMapping';

const TimeRangeType = PropTypes.oneOf(['relative', 'absolute', 'keyword']);
const FieldListType = ImmutablePropTypes.listOf(PropTypes.instanceOf(FieldTypeMapping));

export default Object.assign({ FieldListType, TimeRangeType }, PropTypes);
