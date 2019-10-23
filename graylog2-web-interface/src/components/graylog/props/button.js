import PropTypes from 'prop-types';

const propTypes = {
  bsStyle: PropTypes.oneOf(['success', 'warning', 'danger', 'info', 'default', 'primary', 'link']),
};

const defaultProps = {
  bsStyle: 'default',
};

export { propTypes, defaultProps };
