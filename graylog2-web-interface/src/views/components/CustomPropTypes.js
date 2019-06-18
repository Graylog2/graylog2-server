// @flow strict
import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';

import TFieldType from 'views/logic/fieldtypes/FieldType';
import TFieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

const TimeRangeType = PropTypes.oneOf(['relative', 'absolute', 'keyword']);
const FieldType = PropTypes.instanceOf(TFieldType);
const FieldTypeMapping = PropTypes.instanceOf(TFieldTypeMapping);
const FieldListType = ImmutablePropTypes.listOf(FieldTypeMapping);

const CurrentView = PropTypes.shape({
  activeQuery: PropTypes.string.isRequired,
});

export type CurrentViewType = {
  activeQuery: string,
};

const ValidElements = PropTypes.oneOfType([
  PropTypes.element,
  PropTypes.func,
  PropTypes.string,
]);
const OneOrMoreChildren = PropTypes.oneOfType([
  ValidElements,
  PropTypes.arrayOf(ValidElements),
]);

export default Object.assign({ CurrentView, FieldListType, FieldType, OneOrMoreChildren, TimeRangeType }, PropTypes);
