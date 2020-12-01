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
import React, { useRef, useMemo } from 'react';
import type { ComponentType } from 'react';
import PropTypes from 'prop-types';
import ReactSelect, { components as Components, Creatable as CreatableSelect } from 'react-select';
import { Overlay } from 'react-overlays';
import { useTheme } from 'styled-components';
import { createFilter } from 'react-select/lib/filters';

type Option = { [key: string]: unknown };

const MultiValueRemove = (props) => {
  return (
    <Components.MultiValueRemove {...props}>
      &times;
    </Components.MultiValueRemove>
  );
};

const OverlayInner = ({ children, style }: {
  children: React.ReactElement,
  // eslint-disable-next-line react/require-default-props
  style?: { left: number, top: number }
}) => (
  <>
    {React.Children.map(children,
      (child) => React.cloneElement(child, { style: { ...style, ...child.props.style } }))}
  </>
);

const getRefContainerWidth = (selectRef, allowOptionCreation) => {
  const currentRef = selectRef?.current;
  const containerRef = allowOptionCreation ? currentRef?.select?.select : currentRef?.select;

  return containerRef?.controlRef?.offsetWidth || 0;
};

const menu = (selectRef, allowOptionCreation: boolean) => (base) => {
  const defaultMinWidth = 200;
  const containerWidth = getRefContainerWidth(selectRef, allowOptionCreation);
  const width = containerWidth > defaultMinWidth ? containerWidth : defaultMinWidth;

  return {
    ...base,
    position: 'relative',
    width: `${width}px`,
  };
};

const multiValue = (theme) => (base) => ({
  ...base,
  border: `1px solid ${theme.colors.variant.lighter.info}`,
});

const multiValueLabel = (theme) => (base) => ({
  ...base,
  padding: '2px 5px',
  fontSize: theme.fonts.size.small,
});

const multiValueRemove = (theme) => (base) => ({
  ...base,
  borderLeft: `1px solid ${theme.colors.variant.lighter.info}`,
  paddingLeft: '5px',
  paddingRight: '5px',
  borderRadius: '0',
  ':hover': {
    backgroundColor: `${theme.colors.variant.light.info}`,
  },
});

const option = (base) => ({
  ...base,
  wordWrap: 'break-word',
});

const valueContainer = (base) => ({
  ...base,
  minWidth: '6.5vw',
  minHeight: '30px',
});

const dropdownIndicator = (base, state) => ({
  ...base,
  padding: '0 6px',
  fontSize: '150%',
  transform: state.selectProps.menuIsOpen && 'rotate(180deg)',
});

const clearIndicator = (base) => ({
  ...base,
  padding: '5px',
});

const singleValueAndPlaceholder = (theme) => (base) => ({
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

type Props = {
  allowOptionCreation?: boolean,
  closeMenuOnSelect?: boolean,
  components: { [key: string]: ComponentType<any> },
  escapeClearsValue?: boolean,
  styles: { [key: string]: any },
  inputId?: string,
  ignoreAccents: boolean,
  ignoreCase: boolean,
  options: Array<Option>,
  isDisabled?: boolean,
  isClearable?: boolean,
  isSearchable?: boolean,
  isMulti?: boolean,
  menuShouldScrollIntoView: boolean,
  onChange: (value: unknown, actionType: string) => void,
  placeholder: string,
  value?: unknown,
};

const ValueWithTitle = (props: { data: { label: string } }) => {
  const { data: { label } } = props;

  return <Components.MultiValue {...props} innerProps={{ title: label }} />;
};

const MenuOverlay = (selectRef) => (props) => {
  const listStyle: React.CSSProperties = {
    zIndex: 1050,
    position: 'absolute',
  };

  return (
    <Overlay placement="bottom"
             shouldUpdatePosition
             show
             target={selectRef.current}>
      <OverlayInner>
        <div style={listStyle}>
          <Components.Menu {...props} />
        </div>
      </OverlayInner>
    </Overlay>
  );
};

const Select = ({
  components,
  styles,
  ignoreCase = true,
  ignoreAccents = false,
  allowOptionCreation = false,
  ...rest
}: Props) => {
  const theme = useTheme();
  const selectRef = useRef(null);
  const Component = allowOptionCreation ? CreatableSelect : ReactSelect;
  const Menu = useMemo(() => MenuOverlay(selectRef), [selectRef]);
  const menuStyle = useMemo(() => menu(selectRef, allowOptionCreation), [selectRef, allowOptionCreation]);
  const _components = {
    ...components,
    Menu,
    MultiValueRemove,
    MultiValue: components.MultiValue || ValueWithTitle,
  };
  const _styles = {
    ...styles,
    menu: menuStyle,
    multiValue: multiValue(theme),
    multiValueLabel: multiValueLabel(theme),
    multiValueRemove: multiValueRemove(theme),
    option,
    valueContainer,
    dropdownIndicator,
    clearIndicator,
    singleValue: singleValueAndPlaceholder(theme),
    placeholder: placeholder({ theme }),
  };
  const filterOption = createFilter({ ignoreCase, ignoreAccents });

  const selectTheme = (defaultTheme: {[key: string]: any}) => {
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

  return (
    <Component {...rest}
               components={_components}
               filterOption={filterOption}
               styles={_styles}
               tabSelectsValue={false}
               theme={selectTheme}
               ref={selectRef} />
  );
};

Select.propTypes = {
  allowOptionCreation: PropTypes.bool,
  closeMenuOnSelect: PropTypes.bool,
  components: PropTypes.object,
  escapeClearsValue: PropTypes.bool,
  styles: PropTypes.object,
  inputId: PropTypes.string,
  ignoreAccents: PropTypes.bool,
  ignoreCase: PropTypes.bool,
  isDisabled: PropTypes.bool,
  isClearable: PropTypes.bool,
  isSearchable: PropTypes.bool,
  isMulti: PropTypes.bool,
  menuShouldScrollIntoView: PropTypes.bool,
  options: PropTypes.array.isRequired,
  onChange: PropTypes.func.isRequired,
  value: PropTypes.string,
  placeholder: PropTypes.string,
};

Select.defaultProps = {
  allowOptionCreation: false,
  components: {},
  closeMenuOnSelect: true,
  escapeClearsValue: false,
  styles: {},
  inputId: undefined,
  ignoreAccents: false,
  ignoreCase: true,
  isDisabled: false,
  isClearable: true,
  isSearchable: true,
  isMulti: false,
  // react-select uses !isMobileDevice() by default
  menuShouldScrollIntoView: undefined,
  placeholder: undefined,
  value: undefined,
};

export default Select;
