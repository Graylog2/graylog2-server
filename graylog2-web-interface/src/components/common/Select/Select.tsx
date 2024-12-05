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
import type { Theme as SelectTheme, InputActionMeta, GroupBase, SelectInstance } from 'react-select';
import ReactSelect, { components as Components, createFilter } from 'react-select';
import isEqual from 'lodash/isEqual';
import type { DefaultTheme } from 'styled-components';
import { withTheme } from 'styled-components';
import CreatableSelect from 'react-select/creatable';

import CustomMenuList from 'components/common/Select/CustomMenuList';
import Icon from 'components/common/Icon';
import { INPUT_BORDER_RADIUS } from 'theme/constants';

import AsyncCustomMenuList from './AsyncCustomMenuList';

export const CONTROL_CLASS = 'common-select-control';

type Option = { [key: string]: any };

export type SelectRef = React.Ref<SelectInstance<unknown, boolean, GroupBase<unknown>>>

const MultiValueRemove = ({ children, ...props }: React.ComponentProps<typeof Components.MultiValueRemove>) => (
  <Components.MultiValueRemove {...props}>{children}</Components.MultiValueRemove>
);

const IndicatorSeparator = () => null;

const DropdownIndicator = (props) => {
  const {

    children = <Icon name="arrow_drop_down" />,
    getStyles,
    innerProps: { ref, ...restInnerProps },

  } = props;

  return (
    <div style={getStyles('dropdownIndicator', props)}
         ref={ref}
         {...restInnerProps}>
      {children}
    </div>
  );
};

const Control = ({ children, ...props }: React.ComponentProps<typeof Components.Control>) => (
  <Components.Control {...props} className={CONTROL_CLASS}>{children}</Components.Control>
);

const CustomOption = (optionRenderer: (option: Option, isSelected: boolean) => React.ReactElement) => (
  (props: React.ComponentProps<typeof Components.Option>): React.ReactElement => {
    const { data, isSelected } = props;

    return (
      <Components.Option {...props}>
        {optionRenderer(data, isSelected)}
      </Components.Option>
    );
  }
);

const CustomSingleValue = (valueRenderer: (option: Option) => React.ReactElement) => (props: React.ComponentProps<typeof Components.SingleValue>) => {
  const { data } = props;

  return <Components.SingleValue {...props}>{valueRenderer(data)}</Components.SingleValue>;
};

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
  minHeight: '29px',
  height: '29px',
};

const controlNormal = {
  minHeight: '34px',
};

const menu = (base) => ({
  ...base,
  zIndex: 5,
});

const menuPortal = (base) => ({
  ...base,
  zIndex: 'auto',
});

const singleValueAndPlaceholder = ({ theme }) => (base) => ({
  ...base,
  lineHeight: '28px',
  fontFamily: theme.fonts.family.body,
  fontSize: theme.fonts.size.body,
  fontWeight: 400,
});

const placeholderStyling = ({ theme }) => (base) => ({
  ...base,
  color: theme.colors.input.placeholder,
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
    borderRadius: INPUT_BORDER_RADIUS,
    alignItems: 'center',
  };
};

const valueContainer = ({ size }) => (base) => ({
  ...base,
  padding: size === 'small' ? '0 8px' : '2px 10px',
});

type OverriddenComponents = {
  DropdownIndicator: React.ComponentType<any>;
  MultiValueRemove: React.ComponentType<any>;
  IndicatorSeparator: React.ComponentType<any>;
  Control: React.ComponentType<any>;
};

const _components: OverriddenComponents = {
  DropdownIndicator,
  MultiValueRemove,
  IndicatorSeparator,
  Control,
};

const _styles = ({ size, theme }) => ({
  dropdownIndicator,
  clearIndicator,
  multiValue: multiValue({ theme }),
  multiValueLabel: multiValueLabel({ theme }),
  multiValueRemove: multiValueRemove({ theme }),
  menu,
  menuPortal,
  singleValue: singleValueAndPlaceholder({ theme }),
  placeholder: placeholderStyling({ theme }),
  control: controlFocus({ size, theme }),
  valueContainer: valueContainer({ size }),
});

type ComponentsProp = {
  MultiValueLabel?: React.ComponentType<any>,
  SelectContainer?: React.ComponentType<any>,
};

export type Props<OptionValue> = {
  // The placeholder will be used by default for the aria label.
  'aria-label'?: string,
  addLabelText?: string,
  allowCreate?: boolean,
  autoFocus?: boolean,
  className?: string,
  clearable?: boolean,
  components?: ComponentsProp | null | undefined,
  delimiter?: string,
  disabled?: boolean,
  displayKey?: string,
  forwardedRef?: SelectRef,
  id?: string,
  ignoreAccents?: boolean,
  inputId?: string,
  inputProps?: { [key: string]: any },
  matchProp?: 'any' | 'label' | 'value',
  multi?: boolean,
  maxMenuHeight?: number,
  menuPlacement?: 'bottom' | 'auto' | 'top',
  menuPortalTarget?: HTMLElement,
  menuIsOpen?: boolean,
  name?: string,
  openMenuOnFocus?: boolean,
  onBlur?: (event: React.FocusEvent<HTMLInputElement>) => void,
  onChange: (value: OptionValue) => void,
  onReactSelectChange?: (option: Option | Option[]) => void,
  onMenuClose?: () => void,
  optionRenderer?: (option: Option, isSelected?: boolean) => React.ReactElement,
  options: Array<Option>,
  placeholder?: string,
  persistSelection?: boolean,
  // eslint-disable-next-line react/require-default-props
  ref?: SelectRef,
  size?: 'normal' | 'small',
  theme: DefaultTheme,
  required?: boolean,
  value?: OptionValue,
  valueKey?: string,
  valueRenderer?: (option: Option) => React.ReactElement,
  async?: boolean,
  total?: number,
  onInputChange?: (newValue:string, actionMeta: InputActionMeta) => void,
  loadOptions?: () => void
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

const getCustomComponents = (inputProps?: { [key: string]: any }, optionRenderer?: (option: Option) => React.ReactElement,
  valueRenderer?: (option: Option) => React.ReactElement, async?: boolean): any => {
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

  customComponents.MenuList = async ? AsyncCustomMenuList : CustomMenuList;

  return customComponents;
};

class Select<OptionValue> extends React.Component<Props<OptionValue>, State> {
  static defaultProps = {
    addLabelText: undefined,
    allowCreate: false,
    autoFocus: false,
    className: undefined,
    clearable: true,
    components: null,
    delimiter: ',',
    disabled: false,
    displayKey: 'label',
    persistSelection: true,
    id: undefined,
    ignoreAccents: true,
    inputId: undefined,
    onBlur: undefined,
    inputProps: undefined,
    matchProp: 'any',
    multi: false,
    menuIsOpen: undefined,
    name: undefined,
    openMenuOnFocus: undefined,
    onReactSelectChange: undefined,
    onMenuClose: undefined,
    optionRenderer: undefined,
    placeholder: undefined,
    required: false,
    size: 'normal',
    value: undefined,
    valueKey: 'value',
    valueRenderer: undefined,
    menuPlacement: 'auto',
    maxMenuHeight: 300,
    async: false,
    total: 0,
    onInputChange: undefined,
    loadOptions: undefined,
    menuPortalTarget: undefined,
    forwardedRef: undefined,
  };

  constructor(props: Props<OptionValue>) {
    super(props);
    const { inputProps, optionRenderer, value, valueRenderer, async } = props;

    this.state = {
      customComponents: getCustomComponents(inputProps, optionRenderer, valueRenderer, async),
      value,
    };
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    const { inputProps, optionRenderer, value, valueRenderer, async } = this.props;

    if (value !== nextProps.value) {
      this.setState({ value: nextProps.value });
    }

    if (!isEqual(inputProps, nextProps.inputProps)
      || optionRenderer !== nextProps.optionRenderer
      || valueRenderer !== nextProps.valueRenderer) {
      this.setState({ customComponents: getCustomComponents(inputProps, optionRenderer, valueRenderer, async) });
    }
  }

  // eslint-disable-next-line react/no-unused-class-component-methods
  getValue = () => {
    const { value } = this.state;

    return value;
  };

  // eslint-disable-next-line react/no-unused-class-component-methods
  clearValue = () => {
    this.setState({ value: undefined });
  };

  _extractOptionValue = (onChangeValue: Option | Array<Option>) => {
    const { multi, valueKey, delimiter } = this.props;

    if (onChangeValue) {
      return multi && Array.isArray(onChangeValue) ? onChangeValue.map((i) => i[valueKey]).join(delimiter) : onChangeValue[valueKey || ''];
    }

    return '';
  };

  _onChange = (selectedOption: Option) => {
    const value = this._extractOptionValue(selectedOption);

    if (this.props.persistSelection) {
      this.setState({ value: value });
    }

    const { onChange = () => {} } = this.props;

    onChange(value);
  };

  // Using ReactSelect.Creatable now needs to get values as objects or they are not display
  // This method takes care of formatting a string value into options react-select supports.
  _formatInputValue = (value: OptionValue): Array<Option> => {
    const { options, displayKey, valueKey, delimiter, allowCreate, async } = this.props;

    if (value === undefined || value === null || (typeof value === 'string' && value === '')) {
      return [];
    }

    if ((allowCreate || async) && typeof value === 'string') {
      return value.split(delimiter).map((optionValue: string) => {
        const predicate = {
          [valueKey]: optionValue,
          [displayKey]: optionValue,
        };
        const option = options.find((o) => o[valueKey] === optionValue);

        return option || predicate;
      });
    }

    return (typeof value === 'string'
      ? (value ?? '').split(delimiter)
      : [value])
      .map((v) => {
        const availableOption = options.find((option) => option[valueKey || ''] === v);

        return availableOption ?? { [displayKey]: String(value), [valueKey]: value };
      });
  };

  _selectTheme = (defaultTheme: SelectTheme) => {
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
      displayKey,
      components,
      valueKey,
      onReactSelectChange,
      size,
      theme,
    } = this.props;
    const { customComponents, value } = this.state;

    const formattedValue = this._formatInputValue(value);

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
      menuPortalTarget,
      async,
      total,
      onInputChange,
      loadOptions,
      'aria-label': ariaLabel,
      placeholder,
      ...rest
    } = this.props;

    const stringify = (option) => option[matchProp];
    const customFilter = this.createCustomFilter(stringify);

    const mergedComponents = {
      ..._components,
      ...components,
      ...customComponents,
    };

    const selectProps: (React.ComponentProps<typeof ReactSelect> | React.ComponentProps<typeof CreatableSelect>) & {
      async: boolean,
      loadOptions: () => void,
      total: number,

    } = {
      ...rest,
      onChange: onReactSelectChange || this._onChange,
      onInputChange,
      'aria-label': ariaLabel ?? placeholder,
      placeholder,
      async,
      isMulti,
      isDisabled,
      isClearable,
      loadOptions,
      getOptionLabel: (option: { label?: string }) => option[displayKey] || option.label,
      getOptionValue: (option) => option[valueKey],
      filterOption: customFilter,
      components: mergedComponents,
      menuPortalTarget: menuPortalTarget,
      isOptionDisabled: (option: { disabled?: boolean }) => !!option.disabled,
      styles: _styles({ size, theme }),
      theme: this._selectTheme,
      total,
      value: formattedValue,
    };

    if (allowCreate) {
      return <CreatableSelect ref={rest.forwardedRef} {...selectProps} />;
    }

    return <ReactSelect ref={rest.forwardedRef} {...selectProps} />;
  }
}

export default withTheme(Select) as unknown as React.ComponentType<Omit<Props<any>, 'theme'>>;
