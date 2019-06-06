// @flow strict
import * as React from 'react';
import lodash from 'lodash';
import PropTypes from 'prop-types';

import ReactSelect, { components as Components, Creatable } from 'react-select';
// eslint-disable-next-line import/no-webpack-loader-syntax
import reactSelectSmStyles from '!style/useable!css!./Select.css';

const MultiValueRemove = (props) => {
  return (
    <Components.MultiValueRemove {...props}>
      ×
    </Components.MultiValueRemove>
  );
};

const multiValue = base => ({
  ...base,
  backgroundColor: '#ebf5ff',
  color: '#007eff',
  border: '1px solid rgba(0,126,255,.24)',
});

const multiValueLabel = base => ({
  ...base,
  color: 'unset',
  paddingLeft: '5px',
  paddingRight: '5px',
});

const multiValueRemove = base => ({
  ...base,
  borderLeft: '1px solid rgba(0,126,255,.24)',
  paddingLeft: '5px',
  paddingRight: '5px',
  ':hover': {
    backgroundColor: 'rgba(0,113,230,.08)',
  },
});

const _components = {
  MultiValueRemove,
};
const _styles = {
  multiValue,
  multiValueLabel,
  multiValueRemove,
};

type Option = { [string]: any };
type Props = {
  onChange: (string) => void,
  placeholder: string,
  clearable: boolean,
  displayKey: string,
  valueKey: string,
  delimiter: string,
  options: Array<Option>,
  matchProp: string,
  value: Option | Array<Option>,
  autoFocus: boolean,
  size: 'normal' | 'small',
  optionRenderer: (any) => React.Node,
  disabled: boolean,
  addLabelText: string,
  allowCreate: boolean,
  multi: boolean,
  onReactSelectChange: (Option | Array<Option>) => void,
};

type State = {
  value: any,
};

class Select extends React.Component<Props, State> {
  static propTypes = {
    /**
     * Callback when selected option changes. It receives the value of the
     * selected option as an argument. If `multi` is enabled, the passed
     * argument will be a string separated by `delimiter` with all selected
     * options.
     */
    onChange: PropTypes.func.isRequired,
    /** Size of the select input. */
    size: PropTypes.oneOf(['normal', 'small']),
    /**
     * String containing the selected value. If `multi` is enabled, it must
     * be a string containing all values separated by the `delimiter`.
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
  };

  static defaultProps = {
    multi: false,
    size: 'normal',
    displayKey: 'label',
    valueKey: 'value',
    delimiter: ',',
    allowCreate: false,
    value: undefined,
  };

  constructor(props) {
    super(props);
    const { value } = props;
    this.state = {
      value,
    };
  }

  componentDidMount = () => {
    reactSelectSmStyles.use();
  };

  componentWillReceiveProps = (nextProps) => {
    const { value } = this.props;
    if (value !== nextProps.value) {
      this.setState({ value: nextProps.value });
    }
  };

  componentWillUnmount = () => {
    reactSelectSmStyles.unuse();
  };

  getValue = () => {
    const { value } = this.state;
    return value;
  };

  clearValue = () => {
    this.setState({ value: undefined });
  };

  _extractOptionValue = (option) => {
    const { multi, valueKey, delimiter } = this.props;

    if (option) {
      return multi ? option.map(i => i[valueKey]).join(delimiter) : option[valueKey];
    }
    return '';
  };

  _onChange = (selectedOption) => {
    const value = this._extractOptionValue(selectedOption);
    this.setState({ value: value });

    const { onChange = () => {} } = this.props;

    onChange(value);
  };

  // Using ReactSelect.Creatable now needs to get values as objects or they are not display
  // This method takes care of formatting a string value into options react-select supports.
  _formatInputValue = (value) => {
    const { options, displayKey, valueKey, delimiter } = this.props;

    return value.split(delimiter).map((v) => {
      const predicate = {};
      predicate[valueKey] = v;
      const option = lodash.find(options, predicate);

      predicate[displayKey] = v;
      return option || predicate;
    });
  };

  render() {
    const { allowCreate = false, delimiter, displayKey, size, multi, options, valueKey, onReactSelectChange } = this.props;
    const { value } = this.state;
    const SelectComponent = allowCreate ? Creatable : ReactSelect;

    let formattedValue = value;
    if (value && multi && allowCreate) {
      formattedValue = this._formatInputValue(value);
    } else {
      formattedValue = (value || '').split(delimiter).map(v => options.find(option => option[valueKey] === v));
    }

    const {
      multi: isMulti = false,
      disabled: isDisabled = false,
      ...rest
    } = this.props;

    return (
      <div className={`${size === 'small' ? 'select-sm' : ''} ${reactSelectSmStyles.locals.increaseZIndex}`}>
        <SelectComponent {...rest}
                         onChange={onReactSelectChange || this._onChange}
                         isMulti={isMulti}
                         isDisabled={isDisabled}
                         getOptionLabel={option => option[displayKey]}
                         getOptionValue={option => option[valueKey]}
                         components={_components}
                         styles={_styles}
                         value={formattedValue} />
      </div>
    );
  }
}

export default Select;
