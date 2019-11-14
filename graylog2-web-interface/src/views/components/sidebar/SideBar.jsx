// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { AddWidgetButton } from 'views/components/sidebar';
import { Icon, Spinner } from 'components/common';
import type { ViewMetaData } from 'views/stores/ViewMetadataStore';

import CustomPropTypes from '../CustomPropTypes';
import HighlightingRules from './highlighting/HighlightingRules';
import NavItem from './NavItem';
import ViewDescription from './ViewDescription';

const defaultNewViewTitle = 'New Search';

const Container: React.ComponentType<{ open: boolean }> = styled.div`
  grid-area: sidebar;
  z-index: 3;
  background: #393939;
  color: #9e9e9e;
  height: calc(100vh - 50px);
  padding-top: 20px;
  position: sticky;
  top: 50px;
  grid-column-start: 1;
  grid-column-end: ${props => (props.open ? 3 : 2)};
  box-shadow: 3px 0 3px rgba(0, 0, 0, .25);
`;

const ContentOverlay = styled.div`
  position: fixed;
  top: 0;
  bottom: 0;
  left: 300px;
  right: 0;
  background: rgba(3, 3, 3, 0.25);
`;

const SidebarHeader: React.ComponentType<{open: boolean, hasTitle: boolean}> = styled.div`
  padding: 9px 10px;
  display: flex;
  align-items: center;
  cursor: pointer;
  ${({ open, hasTitle }) => {
    let justifyContent = 'center';
    if (open && hasTitle) justifyContent = 'space-between';
    if (open && !hasTitle) justifyContent = 'flex-end';
    return `justify-content: ${justifyContent}`;
  }}
`;

const Headline = styled.h3`
  color: inherit;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const ToggleIcon = styled.div`
  width: 25px;
  text-align: center;
  font-size: 20px;
  cursor: pointer;
`;

const HorizontalRuler = styled.div`
  width: 100%;
  padding: 0 10px;
  margin: 5px 0 10px 0;
  
  &::after {
    content: ' ';
    display: block;
    width: 100%;
    border-bottom: 1px solid currentColor;
  }
`;

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

  navItemChildren = (navItemChildren: React.Node): React.Node => {
    const { results } = this.props;
    const resultsEmpty = !results || Object.keys(results).length <= 0;
    if (resultsEmpty) {
      return <Spinner />;
    }
    return navItemChildren;
  }

  render() {
    const { results, viewMetadata, children, queryId } = this.props;
    const { open, selectedKey } = this.state;
    const title = viewMetadata.title || defaultNewViewTitle;
    const toggleIcon = open
      ? 'times'
      : 'chevron-right';
    return (
      <Container ref={(node) => { this.wrapperRef = node; }} open={open}>
        {open && <ContentOverlay />}
        <SidebarHeader role="presentation" onClick={this.toggleOpen} hasTitle={!!title} open={open}>
          {open && title && <Headline title={title}>{title}</Headline>}
          <ToggleIcon data-testid="toggle-button"><Icon name={toggleIcon} /></ToggleIcon>
        </SidebarHeader>
        <HorizontalRuler />
        <NavItem isSelected={open && selectedKey === 'viewDescription'}
                 text="View Description"
                 icon="info"
                 onClick={this.setSelectedKey('viewDescription')}
                 isOpen={open}>
          {this.navItemChildren(<ViewDescription viewMetadata={viewMetadata} results={results} />)}
        </NavItem>
        <NavItem isSelected={open && selectedKey === 'createWidget'}
                 text="Create"
                 icon="plus"
                 onClick={this.setSelectedKey('createWidget')}
                 isOpen={open}>
          {this.navItemChildren(<AddWidgetButton onClick={this.toggleOpen} toggleAutoClose={this.toggleAutoClose} queryId={queryId} />)}
        </NavItem>
        <NavItem isSelected={open && selectedKey === 'highlighting'}
                 text="Formatting & Highlighting"
                 icon="paragraph"
                 onClick={this.setSelectedKey('highlighting')}
                 isOpen={open}>
          {this.navItemChildren(<HighlightingRules />)}
        </NavItem>
        <NavItem isSelected={open && selectedKey === 'fields'}
                 text="Fields"
                 icon="subscript"
                 onClick={this.setSelectedKey('fields')}
                 expandRight
                 isOpen={open}>
          {this.navItemChildren(children)}
        </NavItem>
      </Container>
    );
  }
}

export default SideBar;
