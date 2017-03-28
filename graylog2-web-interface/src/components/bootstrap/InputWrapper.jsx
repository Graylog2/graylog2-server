import React from 'react';

const InputWrapper = React.createClass({
  propTypes: {
    className: React.PropTypes.string,
    children: React.PropTypes.oneOfType([
      React.PropTypes.arrayOf(React.PropTypes.oneOfType([React.PropTypes.element, React.PropTypes.string])),
      React.PropTypes.element,
      React.PropTypes.string,
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
