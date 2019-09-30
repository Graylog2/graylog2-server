import PropTypes from 'prop-types';

const propTypes = {
  /* NOTE: need props so we can set default styles */
  active: PropTypes.bool,
  bsStyle: PropTypes.oneOf(['success', 'warning', 'danger', 'info', 'default', 'primary', 'link']),
};

const defaultProps = {
  active: false,
  bsStyle: 'default',
};

export { propTypes, defaultProps };
