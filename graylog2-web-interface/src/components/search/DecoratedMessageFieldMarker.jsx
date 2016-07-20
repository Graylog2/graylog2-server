import React from 'react';

const DecoratedMessageFieldMarker = React.createClass({
  propTypes: {
    title: React.PropTypes.string,
  },
  render() {
    return <i className="fa fa-pencil" title={this.props.title} />;
  },
});

export default DecoratedMessageFieldMarker;
