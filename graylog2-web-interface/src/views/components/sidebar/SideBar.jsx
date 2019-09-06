// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';

// $FlowFixMe: imports from core need to be fixed in flow
import EventHandlersThrottler from 'util/EventHandlersThrottler';
import { Panel, PanelGroup, Button } from 'components/graylog';
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
    open: PropTypes.bool.isRequired,
    toggleOpen: PropTypes.func.isRequired,
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

  _getMaxHeight() {
    return window.innerHeight;
  },

  _updateHeight() {
    const viewPortHeight = this._getMaxHeight();

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

  _getPanelHeader(key) {
    const { open } = this.props;
    return {
      true: {
        viewDescription: 'View Description',
        searchDetails: 'Search Details',
        highlighting: 'Formatting & Highlighting',
        fields: 'Fields',
      },
      false: {
        viewDescription: (<i className={'fa fa-info'} />),
        searchDetails: (<i className={'fa fa-search'} />),
        highlighting: (<i className={'fa fa-paragraph'} />),
        fields: (<i className={'fa fa-subscript'} />),
      }
    }[open][key];
  },

  render() {
    const { children, results, viewMetadata, queryId, toggleOpen, open } = this.props;
    const { activePanel, availableHeight } = this.state;
    const viewDescription = this.formatViewDescription(viewMetadata);
    const toggleClassName = open ? styles.toggleOpen : styles.toggleClose;
    return (
      <div className={styles.sidebarContainer}>
        <div className="sidebar">
          <div className={`${styles.sidebarContent}`} ref={(elem) => { this.sidebar = elem; }}>
            <span className={styles.sidebarNav}>
              <i onClick={toggleOpen} className={`fa fa-chevron-left ${toggleClassName} ${styles.sidebarIcon}`} />
            </span>
            <span className={styles.sidebarNav}>
              <i onClick={toggleOpen} className={`fa fa-info ${styles.sidebarIcon}`} />
              {(open && <div className={styles.sidebarNavFont}>View Description</div>)}
            </span>
            <span className={styles.sidebarNav}>
              <i onClick={toggleOpen} className={`fa fa-search ${styles.sidebarIcon}`} />
              {(open && <div className={styles.sidebarNavFont}>Search Details</div>)}
            </span>
            <span className={styles.sidebarNav}>
              <i onClick={toggleOpen} className={`fa fa-paragraph ${styles.sidebarIcon}`} />
              {(open && <div className={styles.sidebarNavFont}>Formatting and Highlighting</div>)}
            </span>
            <span className={styles.sidebarNav}>
              <i onClick={toggleOpen} className={`fa fa-subscript ${styles.sidebarIcon}`} />
              {(open && <div className={styles.sidebarNavFont}>Fields</div>)}
            </span>
            {/*<PanelGroup accordion activeKey={activePanel} onSelect={newPanel => this.setState({ activePanel: newPanel })}>*/}
            {/*  <Panel eventKey="metadata" header={this._getPanelHeader('viewDescription')}>*/}
            {/*    <span className="pull-right">*/}
            {/*      <AddWidgetButton queryId={queryId} />*/}
            {/*    </span>*/}

            {/*    <div className={styles.viewMetadata}>*/}
            {/*      <h3>{viewMetadata.title || defaultNewViewTitle}</h3>*/}
            {/*      <small>{viewMetadata.summary || defaultNewViewSummary}</small>*/}
            {/*    </div>*/}

            {/*    <div className={styles.viewMetadata}>*/}
            {/*      <SearchResultOverview results={results} />*/}
            {/*    </div>*/}
            {/*    {viewDescription}*/}
            {/*  </Panel>*/}
            {/*  <Panel eventKey="search-details" header={this._getPanelHeader('searchDetails')}>*/}
            {/*    <SearchDetails results={results} />*/}
            {/*  </Panel>*/}
            {/*  <Panel eventKey="decorators" header={this._getPanelHeader('highlighting')}>*/}
            {/*    <HighlightingRules />*/}
            {/*  </Panel>*/}
            {/*  <Panel eventKey="fields" header={this._getPanelHeader('fields')}>*/}
            {/*    {children}*/}
            {/*  </Panel>*/}
            {/*</PanelGroup>*/}
          </div>
        </div>
      </div>
    );
  },
});

export default SideBar;
