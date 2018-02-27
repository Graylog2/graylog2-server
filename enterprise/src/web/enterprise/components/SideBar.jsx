/* eslint-disable react/no-find-dom-node,react/no-string-refs */
/* global window */
import React from 'react';
import ReactDOM from 'react-dom';
import { AutoAffix } from 'react-overlays';

import EventHandlersThrottler from 'util/EventHandlersThrottler';

const SideBar = React.createClass({
  getInitialState() {
    return {
      availableHeight: 1000,
    };
  },

  componentDidMount() {
    this._updateHeight();
    window.addEventListener('resize', this._resizeCallback);
  },

  componentWillUnmount() {
    window.removeEventListener('resize', this._resizeCallback);
  },

  eventsThrottler: new EventHandlersThrottler(),
  SIDEBAR_MARGIN_BOTTOM: 10,

  _resizeCallback() {
    this.eventsThrottler.throttle(() => this._updateHeight());
  },

  _updateHeight() {
    const viewPortHeight = window.innerHeight;

    const sidebar = ReactDOM.findDOMNode(this.refs.sidebar);
    const sidebarCss = window.getComputedStyle(ReactDOM.findDOMNode(sidebar));
    const sidebarPaddingBottom = parseFloat(sidebarCss.getPropertyValue('padding-bottom'));

    const maxHeight = viewPortHeight - sidebarPaddingBottom - this.SIDEBAR_MARGIN_BOTTOM;

    this.setState({ availableHeight: maxHeight });
  },

  render() {
    return (<div style={{ marginTop: -20 }}>
      <AutoAffix viewportOffsetTop={45}>
        <div className="content-col" ref="sidebar">
          {this.props.children}
        </div>
      </AutoAffix>
    </div>);
  },
});

export default SideBar;
