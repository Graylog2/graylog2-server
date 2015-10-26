import React from 'react';
const ReactSelect = require('react-select');

const Select = React.createClass({
  propTypes: ReactSelect.propTypes,
  getInitialState() {
    return {
      value: this.props.value,
    };
  },
  componentDidMount() {
    this.reactSelectStyles.use();
  },
  componentWillUnmount() {
    this.reactSelectStyles.unuse();
  },
  getValue() {
    return this.state.value;
  },
  _onChange(value) {
    this.setState({value: value});
  },
  reactSelectStyles: require('!style/useable!css!react-select/dist/default.css'),
  render() {
    return <ReactSelect onChange={this._onChange} {...this.props} value={this.state.value} />;
  }
});

export default Select;
