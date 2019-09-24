// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { AddWidgetButton, SearchResultOverview } from 'views/components/sidebar';
import { Spinner } from 'components/common';
import type { ViewMetaData } from 'views/stores/ViewMetadataStore';

import styles from './SideBar.css';
import CustomPropTypes from '../CustomPropTypes';
import HighlightingRules from './highlighting/HighlightingRules';
import NavItem from './NavItem';

const defaultNewViewTitle = 'New View';
const defaultNewViewSummary = 'No summary.';

type Props = {
  children: React.Node,
  queryId: string,
  results: {},
  viewMetadata: ViewMetaData,
};

type State = {
  selectedKey: string,
  open: boolean,
  disabledAutoClose: boolean,
};

class SideBar extends React.Component<Props, State> {
  wrapperRef: ?HTMLDivElement;

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
    // $FlowFixMe: EventTarget and Node do work here :(
    if (open && !disabledAutoClose && this.wrapperRef && !this.wrapperRef.contains(event.target)) {
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

  formatViewDescription = (view: ViewMetaData) => {
    const { description } = view;
    if (description) {
      return <span>{description}</span>;
    }
    return <i>No view description.</i>;
  };

  _getNavContent = (key: string) => {
    const { children, results, viewMetadata, queryId } = this.props;
    const viewDescription = this.formatViewDescription(viewMetadata);
    return {
      viewDescription: [
        'View Description',
        (<i className="fa fa-info" />),
        (
          <React.Fragment>
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
      createWidget: [
        'Create',
        (<i className="fa fa-plus" />),
        (<AddWidgetButton onClick={this.toggleOpen} toggleAutoClose={this.toggleAutoClose} queryId={queryId} />),
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


  render() {
    const { results } = this.props;
    const { open, selectedKey } = this.state;
    const gridClass = open ? 'open' : 'closed';
    const resultsEmpty = !results || Object.keys(results).length <= 0;
    const navItems = [
      'viewDescription',
      'createWidget',
      'highlighting',
      'fields',
    ].map((key) => {
      const [text, icon, content] = this._getNavContent(key);
      return (
        <NavItem isSelected={open && selectedKey === key}
                 key={key}
                 text={text}
                 icon={icon}
                 onClick={this.setSelectedKey(key)}
                 isLast={key === 'fields'}
                 isOpen={open}>
          {content}
        </NavItem>
      );
    });

    const shiftToRight = open
      ? styles.iconRight
      : styles.iconLeft;
    const icon = open
      ? 'fa-times'
      : 'fa-chevron-right';
    return (
      <div ref={(node) => { this.wrapperRef = node; }} className={`sidebar-grid ${gridClass}`}>
        <div className={styles.sidebarContainer}>
          <div className="sidebar">
            <div className={`${styles.sidebarContent}`}>
              <span role="presentation" onClick={this.toggleOpen} className={`${styles.sidebarNav} ${shiftToRight}`}>
                <span data-testid="toggle-button"><i className={`fa ${icon} ${styles.sidebarIcon}`} /></span>
              </span>
              {
                resultsEmpty
                  ? <Spinner />
                  : (
                    <React.Fragment>
                      {navItems}
                    </React.Fragment>
                  )
              }
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default SideBar;
