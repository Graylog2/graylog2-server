/* eslint-disable react/no-find-dom-node,react/no-string-refs */
/* global window */
import React from 'react';
import ReactDOM from 'react-dom';
import Reflux from 'reflux';
import { AutoAffix } from 'react-overlays';
import EventHandlersThrottler from 'util/EventHandlersThrottler';

import SearchStore from 'enterprise/stores/SearchStore';

const FieldListSidebar = React.createClass({
  mixins: [Reflux.connect(SearchStore, 'search')],
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
    return (<div style={{ marginTop: -15 }}>
      <AutoAffix viewportOffsetTop={45}>
        <div className="content-col" ref="sidebar">
          <div>
            <h3>Fields</h3>
            <div style={{ height: 1500 }}>&nbsp;</div>
          </div>
        </div>
      </AutoAffix>
    </div>);
  },
});

export default FieldListSidebar;
