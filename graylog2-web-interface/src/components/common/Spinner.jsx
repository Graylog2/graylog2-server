import PropTypes from 'prop-types';
import React from 'react';

const Spinner = React.createClass({
  propTypes: {
    text: PropTypes.string,
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
