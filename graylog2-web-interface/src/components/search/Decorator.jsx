import React from 'react';

const Decorator = React.createClass({
  propTypes: {
    decorator: React.PropTypes.object.isRequired,
  },

  render() {
    const decorator = this.props.decorator;
    return (
      <span>Field <strong>{decorator.field}</strong> is decorated by <strong>{decorator.type}</strong></span>
    );
  },
});

export default Decorator;
