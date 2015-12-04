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
    return (<div><i className="fa fa-spin fa-spinner"/> {this.props.text}</div>);
  },
});

export default Spinner;
