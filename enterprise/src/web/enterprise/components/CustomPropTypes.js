import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';
import TFieldType from 'enterprise/logic/fieldtypes/FieldType';
import TFieldTypeMapping from 'enterprise/logic/fieldtypes/FieldTypeMapping';

const TimeRangeType = PropTypes.oneOf(['relative', 'absolute', 'keyword']);
const FieldType = PropTypes.instanceOf(TFieldType);
const FieldTypeMapping = PropTypes.instanceOf(TFieldTypeMapping);
const FieldListType = ImmutablePropTypes.listOf(FieldTypeMapping);

const CurrentView = PropTypes.shape({
  activeQuery: PropTypes.string.isRequired,
});

const OneOrMoreChildren = PropTypes.oneOfType([
  PropTypes.element,
  PropTypes.arrayOf(PropTypes.element),
]);

export default Object.assign({ CurrentView, FieldListType, FieldType, OneOrMoreChildren, TimeRangeType }, PropTypes);
