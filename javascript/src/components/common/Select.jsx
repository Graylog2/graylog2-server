import React from 'react';
const ReactSelect = require('react-select');

const Select = React.createClass({
  componentDidMount() {
    this.reactSelectStyles.use();
  },
  componentWillUnmount() {
    this.reactSelectStyles.unuse();
  },
  reactSelectStyles: require('react-select/dist/default.css'),
  render() {
    return <ReactSelect {...this.props} />;
  }
});

export default Select;
