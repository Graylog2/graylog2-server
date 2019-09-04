// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';
// $FlowFixMe: imports from core need to be fixed in flow
import { AutoAffix } from 'react-overlays';

// $FlowFixMe: imports from core need to be fixed in flow
import EventHandlersThrottler from 'util/EventHandlersThrottler';
import { Panel, PanelGroup } from 'components/graylog';
import { AddWidgetButton, SearchResultOverview } from 'views/components/sidebar';

import styles from './SideBar.css';
import SearchDetails from './SearchDetails';
import CustomPropTypes from '../CustomPropTypes';
import HighlightingRules from './highlighting/HighlightingRules';

const defaultNewViewTitle = 'New View';
const defaultNewViewSummary = 'No summary.';

const SideBar = createReactClass({
  displayName: 'SideBar',

  propTypes: {
    children: CustomPropTypes.OneOrMoreChildren.isRequired,
    queryId: PropTypes.string.isRequired,
    results: PropTypes.object.isRequired,
    viewMetadata: PropTypes.shape({
      activeQuery: PropTypes.string,
      description: PropTypes.string,
      id: PropTypes.string,
      summary: PropTypes.string,
      title: PropTypes.string,
    }).isRequired,
  },

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

    const sidebarCss = window.getComputedStyle(this.sidebar);
    const sidebarPaddingBottom = parseFloat(sidebarCss.getPropertyValue('padding-bottom'));

    const maxHeight = viewPortHeight - sidebarPaddingBottom - this.SIDEBAR_MARGIN_BOTTOM;

    this.setState({ availableHeight: maxHeight });
  },

  formatViewDescription(view) {
    const { description } = view;
    if (description) {
      return <span>{description}</span>;
    }
    return <i>No view description.</i>;
  },

  render() {
    const { children, results, viewMetadata, queryId } = this.props;
    const { activePanel, availableHeight } = this.state;
    const viewDescription = this.formatViewDescription(viewMetadata);
    return (
      <div className={styles.sidebarContainer}>
        <div id="sidebar">
          <div className={`content-col ${styles.sidebarContent}`} ref={(elem) => { this.sidebar = elem; }}>
              <span className="pull-right">
                <AddWidgetButton queryId={queryId} />
              </span>

            <div className={styles.viewMetadata}>
              <h3>{viewMetadata.title || defaultNewViewTitle}</h3>
              <small>{viewMetadata.summary || defaultNewViewSummary}</small>
            </div>

            <div className={styles.viewMetadata}>
              <SearchResultOverview results={results} />
            </div>

            <PanelGroup accordion activeKey={activePanel} onSelect={newPanel => this.setState({ activePanel: newPanel })}>
              <Panel eventKey="metadata" header="View Description">
                {viewDescription}
              </Panel>
              <Panel eventKey="search-details" header="Search Details">
                <SearchDetails results={results} />
              </Panel>
              <Panel eventKey="decorators" header="Formatting & Highlighting">
                <HighlightingRules />
              </Panel>
              <Panel eventKey="fields" header="Fields">
                {React.cloneElement(children, { maximumHeight: availableHeight })}
              </Panel>
            </PanelGroup>
          </div>
        </div>
      </div>
    );
  },
});

export default SideBar;
