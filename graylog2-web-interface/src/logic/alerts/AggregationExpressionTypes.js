import PropTypes from 'prop-types';

export const internalNodePropType = PropTypes.shape({
  expr: PropTypes.string,
  left: PropTypes.object,
  right: PropTypes.object,
});

export const numberExpressionNodePropType = PropTypes.shape({
  expr: PropTypes.string,
  value: PropTypes.oneOfType([PropTypes.number, PropTypes.string]), // Accept string to allow clearing field
});

export const numberRefNodePropType = PropTypes.shape({
  expr: PropTypes.string,
  ref: PropTypes.string,
});
