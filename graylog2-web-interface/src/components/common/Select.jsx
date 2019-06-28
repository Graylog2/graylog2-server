// @flow strict
import * as React from 'react';
import lodash from 'lodash';
import PropTypes from 'prop-types';

import ReactSelect, { components as Components, Creatable } from 'react-select';

const MultiValueRemove = props => (
  <Components.MultiValueRemove {...props}>
    &times;
  </Components.MultiValueRemove>
);

const IndicatorSeparator = () => null;

const DropdownIndicator = (props) => {
  const {
    /* eslint-disable react/prop-types */
    children = <i className="fa fa-caret-down" />,
    getStyles,
    innerProps: { ref, ...restInnerProps },
    /* eslint-enable react/prop-types */
  } = props;
  return (
    <div style={getStyles('dropdownIndicator', props)}
         ref={ref}
         {...restInnerProps}>
      {children}
    </div>
  );
};

const dropdownIndicator = (base, state) => ({
  ...base,
  padding: '0px',
  fontSize: '150%',
  marginRight: '1rem',
  transform: state.selectProps.menuIsOpen && 'rotate(180deg)',
});

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

const controlSmall = base => ({
  ...base,
  minHeight: '30px',
  height: '30px',
});

const controlNormal = base => ({
  ...base,
  minHeight: '36px',
  height: '36px',
});

const menu = base => ({
  ...base,
  zIndex: 5,
});

const singleValueAndPlaceholder = base => ({
  ...base,
  lineHeight: '28px',
});

const _components = {
  DropdownIndicator,
  MultiValueRemove,
  IndicatorSeparator,
};

const _styles = {
  dropdownIndicator,
  multiValue,
  multiValueLabel,
  multiValueRemove,
  menu,
  singleValue: singleValueAndPlaceholder,
  placeholder: singleValueAndPlaceholder,
};

type Option = { [string]: any };
type Props = {
  onChange: (string) => void,
  placeholder: string,
  clearable: boolean,
  displayKey?: string,
  valueKey?: string,
  delimiter?: string,
  options: Array<Option>,
  matchProp?: string,
  value?: string,
  autoFocus?: boolean,
  size?: 'normal' | 'small',
  optionRenderer: (any) => React.Node,
  disabled?: boolean,
  addLabelText?: string,
  allowCreate?: boolean,
  multi?: boolean,
  onReactSelectChange?: (Option | Array<Option>) => void,
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
    matchProp: undefined,
    autoFocus: false,
    disabled: false,
    addLabelText: undefined,
    onReactSelectChange: undefined,
  };

  constructor(props: Props) {
    super(props);
    const { value } = props;
    this.state = {
      value,
    };
  }

  componentWillReceiveProps = (nextProps: Props) => {
    const { value } = this.props;
    if (value !== nextProps.value) {
      this.setState({ value: nextProps.value });
    }
  };

  getValue = () => {
    const { value } = this.state;
    return value;
  };

  clearValue = () => {
    this.setState({ value: undefined });
  };

  _extractOptionValue = (option: Option) => {
    const { multi, valueKey, delimiter } = this.props;

    if (option) {
      return multi ? option.map(i => i[valueKey]).join(delimiter) : option[valueKey || ''];
    }
    return '';
  };

  _onChange = (selectedOption: Option) => {
    const value = this._extractOptionValue(selectedOption);
    this.setState({ value: value });

    // eslint-disable-next-line no-unused-vars
    const { onChange = (v: string) => {} } = this.props;

    onChange(value);
  };

  // Using ReactSelect.Creatable now needs to get values as objects or they are not display
  // This method takes care of formatting a string value into options react-select supports.
  _formatInputValue = (value: string): Array<Option> => {
    const { options, displayKey, valueKey, delimiter } = this.props;
    return value.split(delimiter).map((v: string) => {
      const predicate: Option = {
        [valueKey || '']: v,
        [displayKey || '']: v,
      };
      const option = lodash.find(options, predicate);

      return option || predicate;
    });
  };

  render() {
    const { allowCreate = false, delimiter, displayKey, size, options, valueKey, onReactSelectChange } = this.props;
    const { value } = this.state;
    const SelectComponent = allowCreate ? Creatable : ReactSelect;

    let formattedValue = value;
    if (value && allowCreate) {
      formattedValue = this._formatInputValue(value);
    } else {
      formattedValue = (value || '').split(delimiter).map(v => options.find(option => option[valueKey || ''] === v));
    }

    const {
      multi: isMulti = false,
      disabled: isDisabled = false,
      ...rest
    } = this.props;

    return (
      <SelectComponent {...rest}
                       onChange={onReactSelectChange || this._onChange}
                       isMulti={isMulti}
                       isDisabled={isDisabled}
                       getOptionLabel={option => option[displayKey]}
                       getOptionValue={option => option[valueKey]}
                       components={{
                         ..._components,
                       }}
                       styles={{
                         ..._styles,
                         control: size === 'small' ? controlSmall : controlNormal,
                       }}
                       value={formattedValue} />
    );
  }
}

export default Select;
