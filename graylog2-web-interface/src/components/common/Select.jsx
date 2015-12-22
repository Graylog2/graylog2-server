import React from 'react';
import ReactSelect from 'react-select';

const propTypes = ReactSelect.propTypes;
propTypes.onValueChange = React.PropTypes.func;

const Select = React.createClass({
  propTypes: propTypes,
  getInitialState() {
    return {
      value: this.props.value,
    };
  },
  componentDidMount() {
    this.reactSelectStyles.use();
  },
  componentWillReceiveProps(nextProps) {
    if (this.props.value !== nextProps.value) {
      this.setState({value: nextProps.value});
    }
  },
  componentWillUnmount() {
    this.reactSelectStyles.unuse();
  },
  getValue() {
    return this.state.value;
  },
  _onChange(value) {
    this.setState({value: value});

    if (this.props.onValueChange) {
      this.props.onValueChange(value);
    }
  },
  reactSelectStyles: require('!style/useable!css!react-select/dist/default.css'),
  render() {
    return <ReactSelect onChange={this._onChange} {...this.props} value={this.state.value} />;
  },
});

export default Select;
