import PropTypes from 'prop-types';

export const Position = PropTypes.shape({
  col: PropTypes.number.isRequired,
  row: PropTypes.number.isRequired,
  height: PropTypes.number.isRequired,
  width: PropTypes.number.isRequired,
});

export const PositionsMap = PropTypes.objectOf(Position);

export const Widget = PropTypes.shape({
  config: PropTypes.object.isRequired,
});

export const WidgetsMap = PropTypes.objectOf(Widget);

export const WidgetData = PropTypes.oneOf(
  PropTypes.arrayOf(
    PropTypes.shape({
      results: PropTypes.array.isRequired,
    }),
  ),
  PropTypes.object,
);

export const WidgetDataMap = PropTypes.objectOf(WidgetData);
