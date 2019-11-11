// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { AddWidgetButton, SearchResultOverview } from 'views/components/sidebar';
import { Icon, Spinner } from 'components/common';
import type { ViewMetaData } from 'views/stores/ViewMetadataStore';

import styles from './SideBar.css';
import CustomPropTypes from '../CustomPropTypes';
import HighlightingRules from './highlighting/HighlightingRules';
import NavItem from './NavItem';

const defaultNewViewTitle = 'New View';
const defaultNewViewSummary = 'No summary.';

type Props = {
  children: React.Element<any>,
  queryId: string,
  results: {},
  viewMetadata: ViewMetaData,
};

type State = {
  selectedKey: string,
  open: boolean,
  disabledAutoClose: boolean,
};

const ViewDescription = (results, viewMetadata) => {
  const formatViewDescription = (view: ViewMetaData) => {
    const { description } = view;
    if (description) {
      return <span>{description}</span>;
    }
    return <i>No view description.</i>;
  };

  return (
    <React.Fragment>
      <div className={styles.viewMetadata}>
        <small>{viewMetadata.summary || defaultNewViewSummary}</small>
      </div>

      <div className={styles.viewMetadata}>
        <SearchResultOverview results={results} />
      </div>
      {formatViewDescription(viewMetadata)}
    </React.Fragment>
  );
};

class SideBar extends React.Component<Props, State> {
  wrapperRef: ?HTMLDivElement;

  static propTypes = {
    children: CustomPropTypes.OneOrMoreChildren.isRequired,
    queryId: PropTypes.string.isRequired,
    results: PropTypes.object,
    viewMetadata: PropTypes.shape({
      activeQuery: PropTypes.string,
      description: PropTypes.string,
      id: PropTypes.string,
      summary: PropTypes.string,
      title: PropTypes.string,
    }).isRequired,
  };

  static defaultProps = {
    results: {},
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      selectedKey: 'fields',
      open: false,
      disabledAutoClose: false,
    };
  }

  componentDidMount() {
    document.addEventListener('mousedown', this.handleClickOutside);
  }

  componentWillUnmount() {
    document.removeEventListener('mousedown', this.handleClickOutside);
  }

  handleClickOutside = (event: MouseEvent) => {
    const { open, disabledAutoClose } = this.state;
    // $FlowFixMe: EventTarget and className work here.
    if (open && !disabledAutoClose && event.target.className.match(/background/)) {
      this.toggleOpen();
    }
  };

  toggleOpen = () => {
    const { open } = this.state;
    this.setState({ open: !open });
  };

  toggleAutoClose = () => {
    const { disabledAutoClose } = this.state;
    this.setState({ disabledAutoClose: !disabledAutoClose });
  };

  setSelectedKey = (key: string) => {
    const { open } = this.state;
    return () => this.setState(
      { selectedKey: key },
      () => !open && this.toggleOpen(),
    );
  };


  render() {
    const { results, viewMetadata, children, queryId } = this.props;
    const { open, selectedKey } = this.state;
    const gridClass = open ? 'open' : 'closed';
    const resultsEmpty = !results || Object.keys(results).length <= 0;
    const title = viewMetadata.title || defaultNewViewTitle;

    const shiftToRight = open
      ? styles.iconRight
      : styles.iconLeft;
    const icon = open
      ? 'times'
      : 'chevron-right';
    return (
      <div ref={(node) => { this.wrapperRef = node; }} className={`sidebar-grid ${gridClass}`}>
        {open && <div className={`background ${styles.toggleArea}`} />}
        <div className={styles.sidebarContainer}>
          <div className="sidebar">
            <div className={styles.sidebarContent}>
              <span role="presentation" onClick={this.toggleOpen} className={`${styles.sidebarNav} ${shiftToRight}`}>
                {open && title && <h3>{title}</h3>}
                <span data-testid="toggle-button"><Icon name={icon} className={styles.sidebarIcon} /></span>
              </span>
              <NavItem isSelected={open && selectedKey === 'viewDescription'}
                       text="View Description"
                       icon={<Icon name="info" />}
                       onClick={this.setSelectedKey('viewDescription')}
                       isLast={false}
                       isOpen={open}>
                {resultsEmpty ? <Spinner /> : <ViewDescription viewMetadata={viewMetadata} results={results} />}
              </NavItem>
              <NavItem isSelected={open && selectedKey === 'createWidget'}
                       text="Create"
                       icon={<Icon name="plus" />}
                       onClick={this.setSelectedKey('createWidget')}
                       isLast={false}
                       isOpen={open}>
                {resultsEmpty ? <Spinner /> : (
                  <AddWidgetButton onClick={this.toggleOpen}
                                   toggleAutoClose={this.toggleAutoClose}
                                   queryId={queryId} />
                )}
              </NavItem>
              <NavItem isSelected={open && selectedKey === 'highlighting'}
                       text="Formatting & Highlighting"
                       icon={<Icon name="paragraph" />}
                       onClick={this.setSelectedKey('highlighting')}
                       isLast={false}
                       isOpen={open}>
                {resultsEmpty ? <Spinner /> : <HighlightingRules />}
              </NavItem>
              <NavItem isSelected={open && selectedKey === 'fields'}
                       text="Fields"
                       icon={<Icon name="subscript" />}
                       onClick={this.setSelectedKey('fields')}
                       isLast
                       isOpen={open}>
                {resultsEmpty ? <Spinner /> : children}
              </NavItem>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default SideBar;
