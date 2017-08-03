import PropTypes from 'prop-types';
import React from 'react';

const InputWrapper = React.createClass({
  propTypes: {
    className: PropTypes.string,
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.oneOfType([PropTypes.element, PropTypes.string])),
      PropTypes.element,
      PropTypes.string,
    ]).isRequired,
  },

  getDefaultProps() {
    return {
      className: undefined,
    };
  },

  render() {
    if (this.props.className) {
      return <div className={this.props.className}>{this.props.children}</div>;
    }
    return <span>{this.props.children}</span>;
  },
});

export default InputWrapper;
