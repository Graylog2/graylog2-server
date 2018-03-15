import PropTypes from 'prop-types';

export const FieldList = PropTypes.arrayOf(
  PropTypes.shape({
    key: PropTypes.string.isRequired,
    value: PropTypes.string.isRequired,
  }),
);

export const AggregationType = PropTypes.shape({
  rowPivots: PropTypes.arrayOf(PropTypes.string),
  series: PropTypes.arrayOf(PropTypes.string),
  sort: PropTypes.arrayOf(PropTypes.string),
  visualization: PropTypes.string,
});
