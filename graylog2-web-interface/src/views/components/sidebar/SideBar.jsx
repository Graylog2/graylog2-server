// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
// $FlowFixMe: could not find types
import { SizeMe } from 'react-sizeme';

import { AddWidgetButton, SearchResultOverview } from 'views/components/sidebar';

import styles from './SideBar.css';
import SearchDetails from './SearchDetails';
import CustomPropTypes from '../CustomPropTypes';
import HighlightingRules from './highlighting/HighlightingRules';

const defaultNewViewTitle = 'New View';
const defaultNewViewSummary = 'No summary.';

type ViewMetaData = {
  activeQuery: string,
  description: string,
  id: string,
  summary: string,
  title: string,
};

type Props = {
  children: React.Node,
  queryId: string,
  results: {},
  viewMetadata: ViewMetaData,
};

type State = {
  selectedKey: string,
  open: boolean,
};

class SideBar extends React.Component<Props, State> {
  static propTypes = {
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
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      selectedKey: 'fields',
      open: false,
    };
  }

  toggleOpen = () => {
    const { open } = this.state;
    this.setState({ open: !open });
  };

  formatViewDescription = (view: ViewMetaData) => {
    const { description } = view;
    if (description) {
      return <span>{description}</span>;
    }
    return <i>No view description.</i>;
  };

  _getPanelHeader = (key: string) => {
    const { children, results, viewMetadata, queryId } = this.props;
    const viewDescription = this.formatViewDescription(viewMetadata);
    return {
      viewDescription: [
        'View Description',
        (<i className="fa fa-info" />),
        (
          <React.Fragment>
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
          </React.Fragment>
        ),
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
  };

  setSelectedKey = (key: string) => {
    const { open } = this.state;
    return () => this.setState(
      { selectedKey: key },
      () => !open && this.toggleOpen(),
    );
  };

  renderNavItem = (key: string) => {
    const { selectedKey, open } = this.state;
    const isSelected = selectedKey === key && open;
    const [text, icon, content] = this._getPanelHeader(key);
    const selectedColor = isSelected ? styles.selected : '';
    // eslint-disable-next-line no-nested-ternary
    const selected = isSelected
      ? (key === 'fields'
        ? styles.openFieldContent
        : styles.contentOpen)
      : styles.contentClosed;

    return (
      <div>
        <div role="presentation" onClick={this.setSelectedKey(key)} className={`${styles.sidebarNav} ${selectedColor}`}>
          <div className={styles.sidebarIcon}>{icon}</div>
          {(open && <div className={styles.sidebarNavFont}>{text}</div>)}
        </div>
        <SizeMe monitorHeight refreshRate={100}>
          {({ size }) => {
            return (
              <div className={`${styles.navContent} ${selected}`}>
                {
                  isSelected
                    ? React.cloneElement(content, { listHeight: size.height - 180 })
                    : <span />
                }
              </div>
            );
          }}
        </SizeMe>
      </div>
    );
  };

  render() {
    const { open } = this.state;
    const toggleClassName = open ? styles.toggleOpen : styles.toggleClose;
    const gridClass = open ? 'open' : 'closed';
    return (
      <div className={`sidebar-grid ${gridClass}`}>
        <div className={styles.sidebarContainer}>
          <div className="sidebar">
            <div className={`${styles.sidebarContent}`}>
              <span role="presentation" onClick={this.toggleOpen} className={styles.sidebarNav}>
                <span data-testid="toggle-button" className={toggleClassName}><i className={`fa fa-chevron-left ${styles.sidebarIcon}`} /></span>
              </span>
              {this.renderNavItem('viewDescription')}
              {this.renderNavItem('searchDetails')}
              {this.renderNavItem('highlighting')}
              {this.renderNavItem('fields')}
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default SideBar;
