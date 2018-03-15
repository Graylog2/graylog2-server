import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import ReactSelect from 'react-select';
import lodash from 'lodash';

import AppConfig from 'util/AppConfig';

// Pass all props react-select accepts, except those we want to override
const filteredProps = ['onChange', 'value', 'labelKey'];
const acceptedReactSelectProps = lodash.without(lodash.keys(ReactSelect.propTypes), ...filteredProps);

/**
 * Component that abstracts a react-select input field. Compared to an standard
 * select field, this one adds the possibility of searching options, which makes
 * it convenient when showing a large number of options. It is also possible to
 * use it as multi-value select, and even to create new options on the fly.
 *
 * You may also pass props directly into react-select, please look at
 * https://github.com/JedWatson/react-select/tree/v1.0.0#usage
 * for more information about the accepted props.
 */
const Select = createReactClass({
  displayName: 'Select',

  propTypes: {
    /**
     * Callback when selected option changes. It receives the value of the
     * selected option as an argument. If `multi` is enabled, the passed
     * argument will be a string separated by `delimiter` with all selected
     * options.
     */
    onChange: PropTypes.func,
    /**
     * @deprecated Please use `onChange` instead.
     */
    onValueChange: PropTypes.func,
    /**
     * Callback when selected option changes. Use this if you want to
     * use react-select directly, receiving the whole option in the callback.
     */
    onReactSelectChange: PropTypes.func,
    /** Size of the select input. */
    size: PropTypes.oneOf(['normal', 'small']),
    /**
     * String containing the selected value. If `multi` is enabled, it must
     * be a string containig all values separated by the `delimiter`.
     */
    value: PropTypes.string,
    /** Specifies if multiple values can be selected or not. */
    multi: PropTypes.bool,
    /** Indicates which option object key contains the text to display in the select input. Same as react-select's `labelKey` prop. */
    displayKey: PropTypes.string,
    /** Indicates which option object key contains the value of the option. */
    valueKey: PropTypes.string,
    /** Delimiter to use as value separator in `multi` Selects. */
    delimiter: PropTypes.string,
    /** Specifies if the user can create new entries in `multi` Selects. */
    allowCreate: PropTypes.bool,
    /**
     * Available options shown in the select field. It should be an array of objects,
     * each one with a display key (specified in `displayKey`), and a value key
     * (specified in `valueKey`).
     */
    options: PropTypes.array.isRequired,
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
    const { allowCreate, displayKey, size, onReactSelectChange, multi } = this.props;
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
                         labelKey={displayKey}
                         value={formattedValue} />
      </div>
    );
  },
});

export default Select;
