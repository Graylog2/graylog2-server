import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';
import Widget from 'enterprise/logic/widgets/Widget';

export const Position = {
  col: PropTypes.number.isRequired,
  row: PropTypes.number.isRequired,
  height: PropTypes.number.isRequired,
  width: PropTypes.number.isRequired,
};

export const PositionsMap = PropTypes.objectOf(PropTypes.shape(Position));

export const ImmutablePositionsMap = ImmutablePropTypes.mapOf(ImmutablePropTypes.mapContains(Position), PropTypes.string);

export const WidgetsMap = PropTypes.objectOf(PropTypes.instanceOf(Widget));

export const ImmutableWidgetsMap = ImmutablePropTypes.mapOf(PropTypes.instanceOf(Widget), PropTypes.string);

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
