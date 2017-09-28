import PropTypes from 'prop-types';
import React from 'react';
import ReactSelect from 'react-select';
import lodash from 'lodash';

import AppConfig from 'util/AppConfig';

// Pass all props react-select accepts, excepting `onChange`
const filteredProps = ['onChange', 'value'];
const acceptedReactSelectProps = lodash.without(lodash.keys(ReactSelect.propTypes), ...filteredProps);

const Select = React.createClass({
  propTypes: {
    onChange: PropTypes.func,
    onValueChange: PropTypes.func, // deprecated
    onReactSelectChange: PropTypes.func,
    size: PropTypes.oneOf(['normal', 'small']),
    value: PropTypes.string,
    multi: PropTypes.bool,
    valueKey: PropTypes.string,
    delimiter: PropTypes.string,
  },

  getDefaultProps() {
    return {
      multi: false,
      size: 'normal',
      valueKey: 'value',
      delimiter: ',',
    };
  },
  getInitialState() {
    return {
      value: this.props.value,
    };
  },
  componentDidMount() {
    this.reactSelectStyles.use();
    this.reactSelectSmStyles.use();
  },
  componentWillReceiveProps(nextProps) {
    if (this.props.value !== nextProps.value) {
      this.setState({ value: nextProps.value });
    }
  },
  componentWillUnmount() {
    this.reactSelectStyles.unuse();
    this.reactSelectSmStyles.unuse();
  },
  getValue() {
    return this.state.value;
  },
  clearValue() {
    // Clear value needs an event, so we just give it one :grumpy:
    // As someone said: "This can't do any more harm that we already do"
    this._select.clearValue(new CustomEvent('fake'));
  },
  // This helps us emulate the behaviour of react-select < 1.0:
  //  - On simple selects it returns the value
  //  - On multi selects it returns all selected values split by a delimiter (',' by default)
  _extractOptionValue(option) {
    const { multi, valueKey, delimiter } = this.props;

    if (option) {
      return multi ? option.map(i => i[valueKey]).join(delimiter) : option[valueKey];
    }
    return '';
  },
  _onChange(selectedOption) {
    const value = this._extractOptionValue(selectedOption);
    this.setState({ value: value });

    if (this.props.onChange) {
      this.props.onChange(value);
    } else if (this.props.onValueChange) {
      if (AppConfig.gl2DevMode()) {
        console.error('Select prop `onValueChange` is deprecated. Please use `onChange` instead.');
      }
      this.props.onValueChange(value);
    }
  },
  _select: undefined,
  reactSelectStyles: require('!style/useable!css!react-select/dist/react-select.css'),
  reactSelectSmStyles: require('!style/useable!css!./Select.css'),
  render() {
    const { size, onReactSelectChange } = this.props;
    const reactSelectProps = lodash.pick(this.props, acceptedReactSelectProps);

    return (
      <div className={size === 'small' ? 'select-sm' : ''}>
        <ReactSelect ref={(c) => { this._select = c; }}
                     onChange={onReactSelectChange || this._onChange}
                     {...reactSelectProps}
                     value={this.state.value} />
      </div>
    );
  },
});

export default Select;
