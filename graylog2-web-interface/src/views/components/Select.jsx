// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import ReactSelect, { components as Components } from 'react-select';

const MultiValueRemove = (props) => {
  return (
    <Components.MultiValueRemove {...props}>
      ×
    </Components.MultiValueRemove>
  );
};

const valueContainer = base => ({
  ...base,
  minWidth: '6.5vw',
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

type Props = {
  components: { [string]: React.ElementType },
  styles: { [string]: any }
};


const ValueWithTitle = (props: {data: { label: string }}) => {
  const { data: { label } } = props;
  return <Components.MultiValue {...props} innerProps={{ title: label }} />;
};

const Select = ({ components, styles, ...rest }: Props) => {
  const _components = {
    MultiValueRemove,
    MultiValue: components.MultiValue || ValueWithTitle,
    ...components,
  };
  const _styles = {
    multiValue,
    multiValueLabel,
    multiValueRemove,
    valueContainer,
    ...styles,
  };
  return <ReactSelect {...rest} components={_components} styles={_styles} tabSelectsValue={false} />;
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
