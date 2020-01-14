// @flow strict
import React, { useRef, useMemo } from 'react';
import type { Node, ElementType } from 'react';
import PropTypes from 'prop-types';
import { get } from 'lodash';
import ReactSelect, { components as Components } from 'react-select';
import { Overlay } from 'react-overlays';

const MultiValueRemove = (props) => {
  return (
    <Components.MultiValueRemove {...props}>
      ×
    </Components.MultiValueRemove>
  );
};

const OverlayInner = ({ children, style }: {children: Node, style?: Object}) => React.Children.map(children,
  child => React.cloneElement(child, { style: Object.assign({}, style, child.props.style) }));

const menu = (selectRef) => {
  const defaultMinWidth = 200;
  const containerWidth = get(selectRef, 'current.select.controlRef.offsetWidth') || 0;
  const width = containerWidth > defaultMinWidth ? containerWidth : defaultMinWidth;
  return base => ({
    ...base,
    position: 'relative',
    width: `${width}px`,
  });
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

const option = base => ({
  ...base,
  wordWrap: 'break-word',
});

const valueContainer = base => ({
  ...base,
  minWidth: '6.5vw',
});

type Props = {
  components: { [string]: ElementType },
  styles: { [string]: any }
};

const ValueWithTitle = (props: {data: { label: string }}) => {
  const { data: { label } } = props;
  return <Components.MultiValue {...props} innerProps={{ title: label }} />;
};

const MenuOverlay = selectRef => (props) => {
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

const Select = ({ components, styles, ...rest }: Props) => {
  const selectRef = useRef(null);
  const Menu = useMemo(() => MenuOverlay(selectRef), [selectRef]);
  const _components = {
    Menu,
    MultiValueRemove,
    MultiValue: components.MultiValue || ValueWithTitle,
    ...components,
  };
  const _styles = {
    menu: menu(selectRef),
    multiValue,
    multiValueLabel,
    multiValueRemove,
    option,
    valueContainer,
    ...styles,
  };
  return <ReactSelect {...rest} components={_components} styles={_styles} tabSelectsValue={false} ref={selectRef} />;
};

Select.propTypes = {
  components: PropTypes.object,
  styles: PropTypes.object,
};

Select.defaultProps = {
  components: {},
  styles: {},
};

export default Select;
