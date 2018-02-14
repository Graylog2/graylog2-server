import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';

import { ListGroupItem } from 'react-bootstrap';

import style from './ControlledTableListHeader.css';

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
    return <div className={style.headerWrapper}>{text}</div>;
  },

  render() {
    const { children } = this.props;

    const header = typeof children === 'string' ? this.wrapStringChildren(children) : children;
    return <ListGroupItem className={style.listGroupHeader}>{header}</ListGroupItem>;
  },
});

export default ControlledTableListHeader;
