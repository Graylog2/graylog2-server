// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { capitalize } from 'lodash';

import type { ViewMetaData } from 'views/stores/ViewMetadataStore';
import type { ViewType } from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';

import { AddWidgetButton } from 'views/components/sidebar';
import { Icon, Spinner } from 'components/common';
import { Container, ContentOverlay, SidebarHeader, Headline, ToggleIcon, HorizontalRuler } from './Sidebar.styles';
import CustomPropTypes from '../CustomPropTypes';
import HighlightingRules from './highlighting/HighlightingRules';
import NavItem from './NavItem';
import ViewDescription from './ViewDescription';

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
    if (open && !disabledAutoClose && event.target.className.match(/background/)) {
      this.toggleOpen();
    }
  };

  toggleOpen = () => {
    const { open, selectedKey } = this.state;
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
    const nextKey = key === selectedKey ? undefined : key;
    this.setState(
      { selectedKey: nextKey },
      () => !open && this.toggleOpen(),
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

  render() {
    const { results, viewMetadata, children, queryId } = this.props;
    const { open, selectedKey } = this.state;

    const toggleIcon = open
      ? 'times'
      : 'chevron-right';
    return (
      <ViewTypeContext.Consumer>
        {(viewType) => {
          const title = this.sidebarTitle(viewType);
          return (
            <Container ref={(node) => { this.wrapperRef = node; }} open={open}>
              {open && <ContentOverlay onClick={this.toggleOpen} />}
              <SidebarHeader role="presentation" onClick={this.toggleOpen} hasTitle={!!title} open={open}>
                {open && title && <Headline title={title}>{title}</Headline>}
                <ToggleIcon><Icon name={toggleIcon} /></ToggleIcon>
              </SidebarHeader>
              <HorizontalRuler />
              <NavItem isSelected={open && selectedKey === 'viewDescription'}
                       text="Description"
                       icon="info"
                       onClick={this.onNavItemClick('viewDescription')}
                       isOpen={open}>
                {this.navItemChildren(<ViewDescription viewMetadata={viewMetadata} results={results} />)}
              </NavItem>
              <NavItem isSelected={open && selectedKey === 'createWidget'}
                       text="Create"
                       icon="plus"
                       onClick={this.onNavItemClick('createWidget')}
                       isOpen={open}>
                {this.navItemChildren(<AddWidgetButton onClick={this.toggleOpen} toggleAutoClose={this.toggleAutoClose} queryId={queryId} />)}
              </NavItem>
              <NavItem isSelected={open && selectedKey === 'highlighting'}
                       text="Formatting & Highlighting"
                       icon="paragraph"
                       onClick={this.onNavItemClick('highlighting')}
                       isOpen={open}>
                {this.navItemChildren(<HighlightingRules />)}
              </NavItem>
              <NavItem isSelected={open && selectedKey === 'fields'}
                       text="Fields"
                       icon="subscript"
                       onClick={this.onNavItemClick('fields')}
                       expandRight
                       isOpen={open}>
                {this.navItemChildren(children)}
              </NavItem>
            </Container>
          );
        }}
      </ViewTypeContext.Consumer>
    );
  }
}

export default SideBar;
