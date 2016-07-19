import React from 'react';

const DecoratedMessageFieldMarker = React.createClass({
  propTypes: {
    onClick: React.PropTypes.func,
    title: React.PropTypes.string,
  },
  getDefaultProps() {
    return {
      onClick: () => {},
      title: 'This field was modified to display differently.',
    };
  },

  render() {
    return <i className="fa fa-pencil" title={this.props.title} onClick={this.props.onClick}/>;
  },
});

export default DecoratedMessageFieldMarker;
