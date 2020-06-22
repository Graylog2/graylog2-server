import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';

import Widget from 'views/logic/widgets/Widget';

import CustomPropTypes from '../CustomPropTypes';

export const Position = {
  col: PropTypes.number.isRequired,
  row: PropTypes.number.isRequired,
  height: PropTypes.number.isRequired,
  width: PropTypes.number.isRequired,
};

export const PositionsMap = PropTypes.objectOf(PropTypes.shape(Position));

export const ImmutablePositionsMap = ImmutablePropTypes.mapOf(ImmutablePropTypes.mapContains(Position), PropTypes.string);

export const WidgetsMap = PropTypes.objectOf(CustomPropTypes.instanceOf(Widget));

export const ImmutableWidgetsMap = ImmutablePropTypes.mapOf(CustomPropTypes.instanceOf(Widget), PropTypes.string);

export const WidgetData = PropTypes.oneOfType([
  PropTypes.arrayOf(PropTypes.object),
  PropTypes.object,
]);

export const WidgetDataMap = PropTypes.objectOf(WidgetData);

export const WidgetError = PropTypes.shape({
  description: PropTypes.string,
});
export const WidgetErrorsList = PropTypes.arrayOf(WidgetError);
export const WidgetErrorsMap = PropTypes.objectOf(WidgetErrorsList);
