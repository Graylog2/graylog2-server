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
    displayKey: PropTypes.string,
    valueKey: PropTypes.string,
    delimiter: PropTypes.string,
    allowCreate: PropTypes.bool,
  },

  getDefaultProps() {
    return {
      multi: false,
      size: 'normal',
      displayKey: 'label',
      valueKey: 'value',
      delimiter: ',',
      allowCreate: false,
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

  // Using ReactSelect.Creatable now needs to get values as objects or they are not display
  // This method takes care of formatting a string value into options react-select supports.
  _formatInputValue(value) {
    const { options, displayKey, valueKey, delimiter } = this.props;

    return value.split(delimiter).map((v) => {
      const predicate = {};
      predicate[valueKey] = v;
      const option = lodash.find(options, predicate);

      predicate[displayKey] = v;
      return option || predicate;
    });
  },
  render() {
    const { allowCreate, size, onReactSelectChange, multi } = this.props;
    const value = this.state.value;
    const reactSelectProps = lodash.pick(this.props, acceptedReactSelectProps);
    const SelectComponent = allowCreate ? ReactSelect.Creatable : ReactSelect;

    let formattedValue = value;
    if (value && multi && allowCreate) {
      formattedValue = this._formatInputValue(value);
    }

    return (
      <div className={size === 'small' ? 'select-sm' : ''}>
        <SelectComponent ref={(c) => { this._select = c; }}
                         onChange={onReactSelectChange || this._onChange}
                         {...reactSelectProps}
                         value={formattedValue} />
      </div>
    );
  },
});

export default Select;
