import React from 'react';
import Select from 'react-select';

const MultiSelect = React.createClass({
  componentDidMount() {
    this.reactSelectStyles.use();
  },
  componentWillUnmount() {
    this.reactSelectStyles.unuse();
  },
  reactSelectStyles: require('react-select/dist/default.css'),
  render() {
    return <Select multi={true} {...this.props} />;
  }
});

export default MultiSelect;
