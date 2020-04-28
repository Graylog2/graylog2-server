import React from 'react';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { ListGroupItem } from 'components/graylog';

const StyledListGroupItem = styled(ListGroupItem)(({ theme }) => `
  &.list-group-item {
    background-color: ${theme.color.global.contentBackground};
    font-size: 14px;
    padding: 0 15px;
  }

  .form-group {
    margin: 0;
  }
`);

const HeaderWrapper = styled.div`
  margin: 10px 0;
  min-height: 20px;
`;

const ControlledTableListHeader = createReactClass({
  propTypes: {
    children: PropTypes.node,
  },

  getDefaultProps() {
    return {
      children: '',
    };
  },

  // We wrap string children to ensure they are displayed properly in the header
  wrapStringChildren(text) {
    return <HeaderWrapper>{text}</HeaderWrapper>;
  },

  render() {
    const { children } = this.props;

    const header = typeof children === 'string' ? this.wrapStringChildren(children) : children;
    return <StyledListGroupItem>{header}</StyledListGroupItem>;
  },
});

export default ControlledTableListHeader;
