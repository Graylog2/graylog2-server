// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { capitalize, isString } from 'lodash';
import styled from 'styled-components';

import type { ViewMetaData } from 'views/stores/ViewMetadataStore';
import type { ViewType } from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import SearchPageLayoutContext from 'views/components/contexts/SearchPageLayoutContext';

import { AddWidgetButton } from 'views/components/sidebar';
import { Icon, Spinner } from 'components/common';
import { Container, ContentOverlay, SidebarHeader, Headline, ToggleIcon, HorizontalRuler } from './Sidebar.styles';
import CustomPropTypes from '../CustomPropTypes';
import HighlightingRules from './highlighting/HighlightingRules';
import NavItem from './NavItem';
import ViewDescription from './ViewDescription';

const PinButton = styled.div`
  bottom: 0;
  position: absolute;
`;

type Props = {
  children: React.Element<any>,
  queryId: string,
  results: {},
  viewMetadata: ViewMetaData,
};

type State = {
  selectedKey?: string,
  open: boolean,
  disabledAutoClose: boolean,
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
      selectedKey: undefined,
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
    const { className } = event.target;
    const canMatchClass = className && isString(className);
    if (open && !disabledAutoClose && (canMatchClass && className.match(/background/))) {
      this.toggleOpen();
    }
  };

  toggleOpen = () => {
    const { open, selectedKey } = this.state;
    const { searchPageLayout, setSearchPageLayout } = this.props;

    this.setState({
      open: !open,
      selectedKey: open ? undefined : selectedKey,
    });
  };

  toggleAutoClose = () => {
    const { disabledAutoClose } = this.state;
    this.setState({ disabledAutoClose: !disabledAutoClose });
  };

  onNavItemClick = (key: string) => () => {
    const { open, selectedKey } = this.state;
    const { searchPageLayout } = this.props;
    const isPinned = searchPageLayout?.sidebar.pinned;

    const nextKey = key === selectedKey ? undefined : key;
    this.setState(
      { selectedKey: nextKey },
      () => !isPinned && !open && this.toggleOpen(),
    );
  };

  navItemChildren = (navItemChildren: React.Element<any>): React.Element<any> => {
    const { results } = this.props;
    const resultsEmpty = !results || Object.keys(results).length <= 0;
    if (resultsEmpty) {
      return <Spinner />;
    }
    return navItemChildren;
  }

  sidebarTitle = (viewType: ?ViewType) => {
    const { viewMetadata } = this.props;
    const defaultTitle = `Untitled ${capitalize(viewType)}`;
    return viewMetadata.title || defaultTitle;
  }

  toggleSidebarPinning = () => {
    const { setSearchPageLayout, searchPageLayout } = this.props;
    const { selectedKey } = this.state;
    const isPinned = searchPageLayout?.sidebar.pinned;
    const updatedLayout = {
      ...searchPageLayout,
      sidebar: { pinned: !isPinned },
    };
    this.setState({
      selectedKey: !isPinned ? selectedKey ?? 'viewDescription' : undefined,
      open: false,
    });
    setSearchPageLayout(updatedLayout);
  }

  render() {
    const { results, viewMetadata, children, queryId, searchPageLayout } = this.props;
    const { open, selectedKey } = this.state;

    const isPinned = searchPageLayout?.sidebar.pinned;

    const toggleIcon = (open || isPinned)
      ? 'times'
      : 'chevron-right';
    return (
      <ViewTypeContext.Consumer>
        {(viewType) => {
          const title = this.sidebarTitle(viewType);
          return (
            <Container ref={(node) => { this.wrapperRef = node; }} open={open} isPinned={searchPageLayout?.sidebar.pinned}>
              {(open && !isPinned) && <ContentOverlay onClick={this.toggleOpen} />}
              <SidebarHeader role="presentation" onClick={isPinned ? this.toggleSidebarPinning : this.toggleOpen} hasTitle={!!title} open={open}>
                {(open && !isPinned) && title && <Headline title={title}>{title}</Headline>}
                <ToggleIcon><Icon name={toggleIcon} /></ToggleIcon>
              </SidebarHeader>
              <HorizontalRuler />
              <NavItem sidebarIsPinned={isPinned}
                       isSelected={(open || isPinned) && selectedKey === 'viewDescription'}
                       text="Description"
                       icon="info"
                       onClick={this.onNavItemClick('viewDescription')}
                       isOpen={open}
                       expandRight={isPinned}>
                {this.navItemChildren(<ViewDescription viewMetadata={viewMetadata} results={results} />)}
              </NavItem>
              <NavItem sidebarIsPinned={isPinned}
                       isSelected={(open || isPinned) && selectedKey === 'createWidget'}
                       text="Create"
                       icon="plus"
                       onClick={this.onNavItemClick('createWidget')}
                       isOpen={open}
                       expandRight={isPinned}>
                {this.navItemChildren(<AddWidgetButton onClick={!isPinned ? this.toggleOpen : () => {}} toggleAutoClose={this.toggleAutoClose} queryId={queryId} />)}
              </NavItem>
              <NavItem sidebarIsPinned={isPinned}
                       isSelected={(open || isPinned) && selectedKey === 'highlighting'}
                       text="Formatting & Highlighting"
                       icon="paragraph"
                       onClick={this.onNavItemClick('highlighting')}
                       isOpen={open}
                       expandRight={isPinned}>
                {this.navItemChildren(<HighlightingRules />)}
              </NavItem>
              <NavItem sidebarIsPinned={isPinned}
                       isSelected={(open || isPinned) && selectedKey === 'fields'}
                       text="Fields"
                       icon="subscript"
                       onClick={this.onNavItemClick('fields')}
                       expandRight
                       isOpen={open}>
                {this.navItemChildren(children)}
              </NavItem>
              <PinButton>
                <NavItem sidebarIsPinned={isPinned}
                         text={isPinned ? 'Unpin Sidebar' : 'Pin Sidebar'}
                         isSelected={isPinned}
                         icon="thumb-tack"
                         onClick={this.toggleSidebarPinning}
                         isOpen={open} />
              </PinButton>
            </Container>
          );
        }}
      </ViewTypeContext.Consumer>
    );
  }
}

const SidebarWithContext = (props: Props) => (
  <SearchPageLayoutContext.Consumer>
    {(searchPageLayout) => {
      return <SideBar {...props} searchPageLayout={searchPageLayout?.layout} setSearchPageLayout={searchPageLayout?.setLayout} />;
    }}
  </SearchPageLayoutContext.Consumer>
);

export default SidebarWithContext;
