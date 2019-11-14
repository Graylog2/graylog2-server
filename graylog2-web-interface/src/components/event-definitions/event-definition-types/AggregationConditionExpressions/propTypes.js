import PropTypes from 'prop-types';

export const internalNodePropType = PropTypes.shape({
  expr: PropTypes.string,
  left: PropTypes.object,
  right: PropTypes.object,
});

