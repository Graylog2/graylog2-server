/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import lodash from 'lodash';
import PropTypes from 'prop-types';
import { DefaultTheme, withTheme } from 'styled-components';
import ReactSelect, { components as Components, Creatable, createFilter } from 'react-select';

import { themePropTypes } from 'theme';

import Icon from './Icon';

type Option = { [key: string]: any };

const MultiValueRemove = (props) => (
  <Components.MultiValueRemove {...props} />
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

type CustomOptionProps = {
  data: any,
};
/* eslint-disable react/prop-types */
const CustomOption = (optionRenderer: (Option) => React.ReactElement) => (
  (props: CustomOptionProps): React.ReactElement => {
    const { data, ...rest } = props;

    return (
      <Components.Option {...rest}>
        {optionRenderer(data)}
      </Components.Option>
    );
  }
);

const CustomSingleValue = (valueRenderer: (option: Option) => React.ReactElement) => (
  ({ data, ...rest }) => <Components.SingleValue {...rest}>{valueRenderer(data)}</Components.SingleValue>
);
/* eslint-enable react/prop-types */

const CustomInput = (inputProps: { [key: string]: any }) => (
  (props) => <Components.Input {...props} {...inputProps} />
);

const dropdownIndicator = (base, state) => ({
  ...base,
  padding: '0px',
  fontSize: '150%',
  marginRight: '1rem',
  transform: state.selectProps.menuIsOpen && 'rotate(180deg)',
});

const clearIndicator = (base) => ({
  ...base,
  padding: '5px',
});

const multiValue = ({ theme }) => (base) => ({
  ...base,
  border: `1px solid ${theme.colors.variant.lighter.info}`,
});

const multiValueLabel = ({ theme }) => (base) => ({
  ...base,
  padding: '2px 5px',
  fontSize: theme.fonts.size.small,
});

const multiValueRemove = ({ theme }) => (base) => ({
  ...base,
  borderLeft: `1px solid ${theme.colors.variant.lighter.info}`,
  paddingLeft: '5px',
  paddingRight: '5px',
  borderRadius: '0',
});

const controlSmall = {
  minHeight: '30px',
  height: '30px',
};

const controlNormal = {
  minHeight: '34px',
};

const menu = (base) => ({
  ...base,
  zIndex: 5,
});

const singleValueAndPlaceholder = ({ theme }) => (base) => ({
  ...base,
  lineHeight: '28px',
  fontFamily: theme.fonts.family.body,
  fontSize: theme.fonts.size.body,
  fontWeight: 400,
});

const placeholder = ({ theme }) => (base) => ({
  ...base,
  lineHeight: '28px',
  fontFamily: theme.fonts.family.body,
  fontSize: theme.fonts.size.body,
  fontWeight: 400,
  whiteSpace: 'nowrap',
  textOverflow: 'ellipsis',
  overflow: 'hidden',
  maxWidth: '100%',
  paddingRight: '20px',
});

const controlFocus = ({ size, theme }) => (base, { isFocused }) => {
  const borderWidth = isFocused ? 1 : base.borderWidth;
  const outline = isFocused ? 0 : base.outline;
  const boxShadow = isFocused ? theme.colors.input.boxShadow : null;
  const controlSize = size === 'small' ? controlSmall : controlNormal;

  return {
    ...base,
    ...controlSize,
    borderWidth,
    boxShadow,
    outline,
    alignItems: 'center',
  };
};

const valueContainer = ({ size }) => (base) => ({
  ...base,
  padding: size === 'small' ? '0 12px' : '2px 12px',
});

type OverriddenComponents = {
  DropdownIndicator: React.ComponentType<any>;
  MultiValueRemove: React.ComponentType<any>;
  IndicatorSeparator: React.ComponentType<any>;
};

const _components: OverriddenComponents = {
  DropdownIndicator,
  MultiValueRemove,
  IndicatorSeparator,
};

const _styles = ({ size, theme }) => ({
  dropdownIndicator,
  clearIndicator,
  multiValue: multiValue({ theme }),
  multiValueLabel: multiValueLabel({ theme }),
  multiValueRemove: multiValueRemove({ theme }),
  menu,
  singleValue: singleValueAndPlaceholder({ theme }),
  placeholder: placeholder({ theme }),
  control: controlFocus({ size, theme }),
  valueContainer: valueContainer({ size }),
});

type ComponentsProp = {
  MultiValueLabel?: React.ComponentType<any>,
};

type Props = {
  addLabelText?: string,
  allowCreate?: boolean,
  autoFocus?: boolean,
  clearable?: boolean,
  components?: ComponentsProp | null | undefined,
  delimiter?: string,
  disabled?: boolean,
  displayKey: string,
  id?: string,
  ignoreAccents?: boolean,
  inputId?: string,
  inputProps?: { [key: string]: any },
  matchProp?: 'any' | 'label' | 'value',
  multi?: boolean,
  name?: string,
  onBlur?: (event: React.FocusEvent<HTMLInputElement>) => void,
  onChange: (string) => void,
  onReactSelectChange?: (option: Option | Option[]) => void,
  optionRenderer?: (option: Option) => React.ReactElement,
  options: Array<Option>,
  placeholder: string,
  ref?: React.Ref<React.ComponentType>,
  size?: 'normal' | 'small',
  theme: DefaultTheme,
  value?: Object | Array<Object> | null | undefined,
  valueKey: string,
  valueRenderer?: (option: Option) => React.ReactElement,
};

type CustomComponents = {
  Input?: React.ComponentType<any>,
  Option?: React.ComponentType<any>,
  SingleValue?: React.ComponentType<any>,
};

type State = {
  customComponents: CustomComponents,
  value: any,
};

class Select extends React.Component<Props, State> {
  static propTypes = {
    /** Specifies if the user can create new entries in `multi` Selects. */
    allowCreate: PropTypes.bool,
    /** Indicates if the Select value is clearable or not. */
    clearable: PropTypes.bool,
    /**
     * A collection of custom `react-select` components from https://react-select.com/components
     */
    components: PropTypes.objectOf(PropTypes.elementType),
    /** Delimiter to use as value separator in `multi` Selects. */
    delimiter: PropTypes.string,
    /** Indicates whether the Select component is disabled or not. */
    disabled: PropTypes.bool,
    /** Indicates which option object key contains the text to display in the select input. Same as react-select's `labelKey` prop. */
    displayKey: PropTypes.string,
    /** ID of Select container component */
    id: PropTypes.string,
    /** ID of underlying input */
    inputId: PropTypes.string,
    /** Indicates whether the auto-completion should return results including accents/diacritics when searching for their non-accent counterpart */
    ignoreAccents: PropTypes.bool,
    /**
     * @deprecated Use `inputId` or custom components with the `components` prop instead.
     * Custom attributes for the input (inside the Select).
     */
    inputProps: PropTypes.object,
    /** Indicates which option property to filter on. */
    matchProp: PropTypes.oneOf(['any', 'label', 'value']),
    /** Specifies if multiple values can be selected or not. */
    multi: PropTypes.bool,
    /** name attribute for Select element */
    name: PropTypes.string,
    /** Callback when select has lost focus */
    onBlur: PropTypes.func,
    /**
     * Callback when selected option changes. It receives the value of the
     * selected option as an argument. If `multi` is enabled, the passed
     * argument will be a string separated by `delimiter` with all selected
     * options.
     */
    onChange: PropTypes.func.isRequired,
    /**
     * Available options shown in the select field. It should be an array of objects,
     * each one with a display key (specified in `displayKey`), and a value key
     * (specified in `valueKey`).
     * Options including an optional `disabled: true` key-value pair, will be disabled in the Select component.
     */
    options: PropTypes.array.isRequired,
    /** Custom function to render the options in the menu. */
    optionRenderer: PropTypes.func,
    /** Size of the select input. */
    size: PropTypes.oneOf(['normal', 'small']),
    /** @ignore */
    theme: themePropTypes.isRequired,
    /**
     * Value which can be the selected option or the value of the selected option.
     * If `multi` is enabled, it must be a string containing all values separated by the `delimiter`.
     */
    value: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.object,
      PropTypes.arrayOf(PropTypes.object),
    ]),
    /** Indicates which option object key contains the value of the option. */
    valueKey: PropTypes.string,
    /** Custom function to render the selected option in the Select. */
    valueRenderer: PropTypes.func,
    /** Label text for add button */
    addLabelText: PropTypes.string,
    /** Automatically Focus on Select */
    autoFocus: PropTypes.bool,
    /** special onChange handler */
    onReactSelectChange: PropTypes.func,
    /** Select placeholder text */
    placeholder: PropTypes.string,
    /** Placement of the menu: "top", "bottom", "auto" */
    menuPlacement: PropTypes.oneOf(['top', 'bottom', 'auto']),
    /** Max height of the menu */
    maxMenuHeight: PropTypes.number,
  }

  static defaultProps = {
    addLabelText: undefined,
    allowCreate: false,
    autoFocus: false,
    clearable: true,
    components: null,
    delimiter: ',',
    disabled: false,
    displayKey: 'label',
    id: undefined,
    ignoreAccents: true,
    inputId: undefined,
    onBlur: undefined,
    inputProps: undefined,
    matchProp: 'any',
    multi: false,
    name: undefined,
    onReactSelectChange: undefined,
    optionRenderer: undefined,
    placeholder: undefined,
    size: 'normal',
    value: undefined,
    valueKey: 'value',
    valueRenderer: undefined,
    menuPlacement: 'bottom',
    maxMenuHeight: 300,
  };

  constructor(props: Props) {
    super(props);
    const { inputProps, optionRenderer, value, valueRenderer } = props;

    this.state = {
      customComponents: this.getCustomComponents(inputProps, optionRenderer, valueRenderer),
      value,
    };
  }

  UNSAFE_componentWillReceiveProps = (nextProps: Props) => {
    const { inputProps, optionRenderer, value, valueRenderer } = this.props;

    if (value !== nextProps.value) {
      this.setState({ value: nextProps.value });
    }

    if (inputProps !== nextProps.inputProps
      || optionRenderer !== nextProps.optionRenderer
      || valueRenderer !== nextProps.valueRenderer) {
      this.setState({ customComponents: this.getCustomComponents(inputProps, optionRenderer, valueRenderer) });
    }
  };

  getCustomComponents = (inputProps?: { [key: string]: any }, optionRenderer?: (option: Option) => React.ReactElement,
    valueRenderer?: (option: Option) => React.ReactElement): any => {
    const customComponents: { [key: string]: any } = {};

    if (inputProps) {
      customComponents.Input = CustomInput(inputProps);
    }

    if (optionRenderer) {
      customComponents.Option = CustomOption(optionRenderer);
    }

    if (valueRenderer) {
      customComponents.SingleValue = CustomSingleValue(valueRenderer);
    }

    return customComponents;
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
      return multi ? option.map((i) => i[valueKey]).join(delimiter) : option[valueKey || ''];
    }

    return '';
  };

  _onChange = (selectedOption: Option) => {
    const value = this._extractOptionValue(selectedOption);

    this.setState({ value: value });

    // eslint-disable-next-line @typescript-eslint/no-empty-function
    const { onChange = () => {} } = this.props;

    onChange(value);
  };

  // Using ReactSelect.Creatable now needs to get values as objects or they are not display
  // This method takes care of formatting a string value into options react-select supports.
  _formatInputValue = (value: string): Array<Option> => {
    const { options, displayKey, valueKey, delimiter } = this.props;

    return value.split(delimiter).map((v: string) => {
      const predicate: Option = {
        [valueKey]: v,
        [displayKey]: v,
      };
      const option = lodash.find(options, predicate);

      return option || predicate;
    });
  };

  _selectTheme = (defaultTheme: {[key: string]: any}) => {
    const { theme } = this.props;

    return {
      ...defaultTheme,
      colors: {
        ...defaultTheme.colors,
        primary: theme.colors.input.borderFocus,
        primary75: theme.colors.variant.light.default,
        primary50: theme.colors.variant.lighter.default,
        primary25: theme.colors.variant.lightest.default,
        danger: theme.colors.variant.darker.info,
        dangerLight: theme.colors.variant.lighter.info,
        neutral0: theme.colors.input.background,
        neutral5: theme.colors.input.backgroundDisabled,
        neutral10: theme.colors.variant.lightest.info,
        neutral20: theme.colors.input.border,
        neutral30: theme.colors.gray[70],
        neutral40: theme.colors.gray[60],
        neutral50: theme.colors.gray[50],
        neutral60: theme.colors.gray[40],
        neutral70: theme.colors.gray[30],
        neutral80: theme.colors.gray[20],
        neutral90: theme.colors.gray[10],
      },
    };
  };

  createCustomFilter = (stringify: (any) => string) => {
    const { matchProp, ignoreAccents } = this.props;
    const options = { ignoreAccents };

    return matchProp === 'any' ? createFilter(options) : createFilter({ ...options, stringify });
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
    const { customComponents, value } = this.state;
    const SelectComponent = allowCreate ? Creatable : ReactSelect;

    let formattedValue = value;

    if (formattedValue && allowCreate) {
      formattedValue = this._formatInputValue(value);
    } else {
      formattedValue = (typeof value === 'string'
        ? (value ?? '').split(delimiter)
        : [value])
        .map((v) => options.find((option) => option[valueKey || ''] === v));
    }

    const {
      multi: isMulti,
      disabled: isDisabled,
      clearable: isClearable,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      inputProps, // Do not pass down prop
      matchProp,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      optionRenderer, // Do not pass down prop
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      valueRenderer, // Do not pass down prop
      ...rest
    } = this.props;

    const stringify = (option) => option[matchProp];
    const customFilter = this.createCustomFilter(stringify);

    const mergedComponents = {
      ..._components,
      ...components,
      ...customComponents,
    };

    return (
      <SelectComponent {...rest}
                       onChange={onReactSelectChange || this._onChange}
                       isMulti={isMulti}
                       isDisabled={isDisabled}
                       isClearable={isClearable}
                       getOptionLabel={(option) => option[displayKey]}
                       getOptionValue={(option) => option[valueKey]}
                       filterOption={customFilter}
                       components={mergedComponents}
                       isOptionDisabled={(option) => !!option.disabled}
                       /* eslint-disable-next-line @typescript-eslint/ban-ts-comment */
                       // @ts-ignore TODO: Fix props assignment for _styles
                       styles={_styles(this.props)}
                       theme={this._selectTheme}
                       value={formattedValue} />
    );
  }
}

export default withTheme(Select);
