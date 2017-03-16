import React from 'react';

const Spinner = React.createClass({
  propTypes: {
    text: React.PropTypes.string,
  },
  getDefaultProps() {
    return { text: 'Loading...' };
  },
  getInitialState() {
    return {};
  },
  render() {
    return (<span><i className="fa fa-spin fa-spinner" /> {this.props.text}</span>);
  },
});

export default Spinner;
