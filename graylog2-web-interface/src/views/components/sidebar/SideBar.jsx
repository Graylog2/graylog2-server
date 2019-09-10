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
      selectedKey: undefined,
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
    const { children, results, viewMetadata, queryId } = this.props;
    const viewDescription = this.formatViewDescription(viewMetadata);
    return {
      viewDescription: [
        'View Description',
        (<i className="fa fa-info" />),
        (<React.Fragment>
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
          {viewDescription}
        </React.Fragment>)
      ],
      searchDetails: [
        'Search Details',
        (<i className="fa fa-search" />),
        (<SearchDetails results={results} />),
      ],
      highlighting: [
        'Formatting & Highlighting',
        (<i className="fa fa-paragraph" />),
        <HighlightingRules />,
      ],
      fields: [
        'Fields',
        (<i className="fa fa-subscript" />),
        children,
      ],
    }[key];
  },

  setSelectedKey(key) {
    return () => this.setState({ selectedKey: key });
  },

  renderNavItem(key) {
    const { open } = this.props;
    const { selectedKey } = this.state;
    const isSelected = selectedKey === key && open;
    const [text, icon, content] = this._getPanelHeader(key);
    const selectedColor = isSelected ? styles.selected : '';
    const selected = isSelected ? styles.contentOpen : styles.contentClosed;
    const openContent = isSelected ? content : '';

    return (
      <div>
        <div onClick={this.setSelectedKey(key)} className={`${styles.sidebarNav} ${selectedColor}`}>
          <div className={styles.sidebarIcon}>{icon}</div>
          {(open && <div className={styles.sidebarNavFont}>{text}</div>)}
        </div>
        <div className={`${styles.navContent} ${selected}`}>{openContent}</div>
      </div>
  );
  },

  render() {
    const { toggleOpen, open } = this.props;
    const toggleClassName = open ? styles.toggleOpen : styles.toggleClose;
    return (
      <div className={styles.sidebarContainer}>
        <div className="sidebar">
          <div className={`${styles.sidebarContent}`} ref={(elem) => { this.sidebar = elem; }}>
            <span className={styles.sidebarNav}>
              <span><i onClick={toggleOpen} className={`fa fa-chevron-left ${toggleClassName} ${styles.sidebarIcon}`} /></span>
            </span>
            {this.renderNavItem('viewDescription')}
            {this.renderNavItem('searchDetails')}
            {this.renderNavItem('highlighting')}
            {this.renderNavItem('fields')}
          </div>
        </div>
      </div>
    );
  },
});

export default SideBar;
