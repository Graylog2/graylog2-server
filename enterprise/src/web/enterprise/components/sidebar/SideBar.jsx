/* eslint-disable react/no-find-dom-node,react/no-string-refs */
/* global window */
import React from 'react';
import createReactClass from 'create-react-class';
import ReactDOM from 'react-dom';
import { AutoAffix } from 'react-overlays';

import EventHandlersThrottler from 'util/EventHandlersThrottler';
import { Panel, PanelGroup } from 'react-bootstrap';
import { AddWidgetButton, SearchResultOverview } from 'enterprise/components/sidebar';

import styles from './SideBar.css';

const defaultNewViewTitle = 'New View';
const defaultNewViewSummary = 'No summary.';

const SideBar = createReactClass({
  displayName: 'SideBar',

  getInitialState() {
    return {
      availableHeight: 1000,
      activePanel: 'fields',
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
  SIDEBAR_MARGIN_BOTTOM: 40,

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

  formatViewDescription(view) {
    const { description } = view.toObject();
    if (description) {
      return <span>{description}</span>;
    }
    return <i>No view description.</i>;
  },

  render() {
    const { results, view, viewId, queryId } = this.props;
    const viewDescription = this.formatViewDescription(view);
    return (
      <div className={styles.sidebarContainer}>
        <AutoAffix viewportOffsetTop={46}>
          <div id="sidebar">
            <div className={`content-col ${styles.sidebarContent}`} ref="sidebar">
              <span className={"pull-right"}>
                <AddWidgetButton viewId={viewId} queryId={queryId} />
              </span>

              <div className={styles.viewMetadata}>
                <h3>{view.get('title') || defaultNewViewTitle}</h3>
                <small>{view.get('summary') || defaultNewViewSummary}</small>
              </div>

              <div className={styles.viewMetadata}>
                <SearchResultOverview results={results} />
              </div>

              <PanelGroup accordion activeKey={this.state.activePanel} onSelect={newPanel => this.setState({ activePanel: newPanel })}>
                <Panel eventKey="metadata" header="View Description">
                  {viewDescription}
                </Panel>
                <Panel eventKey="fields" header="Fields">
                  {this.props.children({ maximumHeight: this.state.availableHeight })}
                </Panel>
              </PanelGroup>
            </div>
          </div>
        </AutoAffix>
      </div>
    );
  },
});

export default SideBar;
