// @flow strict
import React, { useRef, useMemo } from 'react';
import type { Node, ComponentType } from 'react';
import PropTypes from 'prop-types';
import ReactSelect, { components as Components, Creatable as CreatableSelect } from 'react-select';
import { Overlay } from 'react-overlays';
import { withTheme } from 'styled-components';
import { createFilter } from 'react-select/lib/filters';

import { themePropTypes, type ThemeInterface } from 'theme';

const MultiValueRemove = (props) => {
  return (
    <Components.MultiValueRemove {...props}>
      &times;
    </Components.MultiValueRemove>
  );
};

const OverlayInner = ({ children, style }: {children: Node, style?: { left: number, top: number }}) => React.Children.map(children,
  (child) => React.cloneElement(child, { style: { ...style, ...child.props.style } }));

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

type Props = {
  allowOptionCreation?: boolean,
  components: { [string]: ComponentType<any> },
  styles: { [string]: any },
  ignoreAccents: boolean,
  ignoreCase: boolean,
  theme: ThemeInterface,
};

const ValueWithTitle = (props: { data: { label: string } }) => {
  const { data: { label } } = props;

  return <Components.MultiValue {...props} innerProps={{ title: label }} />;
};

const MenuOverlay = (selectRef) => (props) => {
  const listStyle = {
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
  theme,
  ...rest
}: Props) => {
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
  };
  const filterOption = createFilter({ ignoreCase, ignoreAccents });

  const selectTheme = (defaultTheme: {[string]: any}) => {
    return {
      ...defaultTheme,
      colors: {
        ...defaultTheme.colors,
        primary: theme.colors.variant.light.info,
        primary75: theme.colors.variant.light.default,
        primary50: theme.colors.variant.lighter.default,
        primary25: theme.colors.variant.lightest.default,
        danger: theme.colors.variant.darker.info,
        dangerLight: theme.colors.variant.lighter.info,
        neutral0: theme.colors.gray[100],
        neutral5: theme.colors.gray[90],
        neutral10: theme.colors.variant.lightest.info,
        neutral20: theme.colors.gray[80],
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
  components: PropTypes.object,
  styles: PropTypes.object,
  ignoreAccents: PropTypes.bool,
  ignoreCase: PropTypes.bool,
  theme: themePropTypes.isRequired,
};

Select.defaultProps = {
  allowOptionCreation: false,
  components: {},
  styles: {},
  ignoreAccents: false,
  ignoreCase: true,
};

export default withTheme(Select);
