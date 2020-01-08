// @flow strict
import * as React from 'react';
import lodash from 'lodash';
import PropTypes from 'prop-types';
import ReactSelect, { components as Components, Creatable } from 'react-select';

import Icon from './Icon';

const MultiValueRemove = props => (
  <Components.MultiValueRemove {...props}>
    &times;
  </Components.MultiValueRemove>
);

const IndicatorSeparator = () => null;

const DropdownIndicator = (props) => {
  const {
    /* eslint-disable react/prop-types */
    children = <Icon name="caret-down" />,
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

const clearIndicator = base => ({
  ...base,
  padding: '5px',
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

const controlSmall = {
  minHeight: '30px',
  height: '30px',
};

const controlNormal = {
  minHeight: '34px',
};

const menu = base => ({
  ...base,
  zIndex: 5,
  border: '1px solid rgba(102, 175, 233, 0.5)',
  boxShadow: '0 0 4px rgba(102, 175, 233, 0.3)',
});

const singleValueAndPlaceholder = base => ({
  ...base,
  lineHeight: '28px',
  fontFamily: '"Open Sans", "Helvetica Neue", Helvetica, Arial, sans-serif',
  fontSize: '14px',
  fontWeight: 400,
  color: '#666',
});

const placeholder = base => ({
  ...base,
  lineHeight: '28px',
  fontFamily: '"Open Sans", "Helvetica Neue", Helvetica, Arial, sans-serif',
  fontSize: '14px',
  fontWeight: 400,
  color: '#999',
  whiteSpace: 'nowrap',
  textOverflow: 'ellipsis',
  overflow: 'hidden',
  maxWidth: '100%',
  paddingRight: '20px',
});

const controlFocus = props => (base, { isFocused }) => {
  const { size } = props;

  const borderColor = isFocused ? '#66afe9' : base.borderColor;
  const borderWidth = isFocused ? 1 : base.borderWidth;
  const outline = isFocused ? 0 : base.outline;
  const boxShadow = isFocused ? 'inset 0 1px 1px rgba(0, 0, 0, .075), 0 0 8px rgba(102, 175, 233, 0.6)' : 'inset 0 1px 1px rgba(0, 0, 0, 0.075)';

  const controlSize = size === 'small' ? controlSmall : controlNormal;

  return {
    ...base,
    ...controlSize,
    borderColor,
    borderWidth,
    outline,
    boxShadow,
    alignItems: 'center',
  };
};

const valueContainer = base => ({
  ...base,
  padding: '2px 12px',
});

const _components = {
  DropdownIndicator,
  MultiValueRemove,
  IndicatorSeparator,
};

const _styles = props => ({
  dropdownIndicator,
  clearIndicator,
  multiValue,
  multiValueLabel,
  multiValueRemove,
  menu,
  singleValue: singleValueAndPlaceholder,
  placeholder,
  control: controlFocus(props),
  valueContainer,
});

type Option = { [string]: any };
type Props = {
  onChange: (string) => void,
  placeholder: string,
  clearable?: boolean,
  displayKey?: string,
  valueKey?: string,
  delimiter?: string,
  options: Array<Option>,
  components: ?{string: React.Node},
  matchProp?: string,
  value?: string,
  autoFocus?: boolean,
  size?: 'normal' | 'small',
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
    /**
     * A collection of custom `react-select` components from https://react-select.com/components
     */
    components: PropTypes.arrayOf(PropTypes.node),
    disabled: PropTypes.bool,
    clearable: PropTypes.bool,
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
    components: null,
    clearable: true,
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
    const {
      allowCreate = false,
      delimiter,
      displayKey,
      components,
      options,
      valueKey,
      onReactSelectChange,
    } = this.props;
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
      clearable: isClearable,
      ...rest
    } = this.props;

    return (
      <SelectComponent {...rest}
                       onChange={onReactSelectChange || this._onChange}
                       isMulti={isMulti}
                       isDisabled={isDisabled}
                       isClearable={isClearable}
                       getOptionLabel={option => option[displayKey]}
                       getOptionValue={option => option[valueKey]}
                       components={{
                         ..._components,
                         ...components,
                       }}
                       styles={{
                         ..._styles(this.props),
                       }}
                       value={formattedValue} />
    );
  }
}

export default Select;
