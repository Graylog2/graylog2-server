import React from 'react';
import PropTypes from 'prop-types';

import UIUtils from 'util/UIUtils';

export default class ScrollToHint extends React.Component {
  static propTypes = {
    children: PropTypes.node.isRequired,
    value: PropTypes.any.isRequired,
  };

  componentWillReceiveProps(nextProps) {
    if (!this.element) {
      return;
    }

    if (this.props.value !== nextProps.value) {
      UIUtils.scrollToHint(this.element);
    }
  }

  render() {
    const { children } = this.props;

    return (
      <span ref={(element) => { this.element = element; }}>
        {children}
      </span>
    );
  }
}
