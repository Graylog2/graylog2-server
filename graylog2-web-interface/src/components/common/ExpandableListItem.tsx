/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { Checkbox } from 'components/bootstrap';

import Icon from './Icon';

const ItemWrap = styled.li(({ padded }: { padded: boolean }) => css`
  padding: ${padded ? '10px 5px' : ''};
`);

const Container = styled.div(({ theme }) => css`
  display: flex;
  font-size: ${theme.fonts.size.body};
  line-height: 20px;

  label {
    min-height: 20px;
    margin-bottom: 2px;
    margin-right: 5px;

    * {
      cursor: pointer;
    }
  }
`);

const Toggle = styled.div`
  display: inline-block;
  width: 20px;
  margin-right: 5px;
`;

const IconStack = styled.div(({ theme }) => css`
  &.fa-stack {
    cursor: pointer;
    font-size: ${theme.fonts.size.large};
    line-height: 20px;
    width: 1em;
    height: 1em;
    vertical-align: text-top;

    &:hover [class*='fa-'] {
      color: ${theme.colors.variant.primary};
      opacity: 1;
    }
  }

  [class*='fa-']:first-child {
    opacity: 0;

    ~ [class*='fa-']:hover {
      color: ${theme.colors.global.contentBackground};
    }
  }
`);

const HeaderWrap = styled.span(({ theme }) => css`
  font-size: ${theme.fonts.size.large};
`);

const Header = styled.button`
  display: flex;
  border: 0;
  padding: 0;
  text-align: left;
  background: transparent;
`;

const Subheader = styled.span(({ theme }) => css`
  font-size: ${theme.fonts.size.body};
  margin-left: 0.5em;
  color: ${theme.colors.gray[70]};
`);

const ExpandableContent = styled.div(({ theme }) => css`
  border-left: 1px ${theme.colors.gray[90]} solid;
  margin-left: 35px;
  margin-top: 10px;
  padding-left: 5px;
`);

const _filterInputProps = (props) => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { expanded, indetermined, stayExpanded, padded, ...inputProps } = props;

  return inputProps;
};

type Props = React.ComponentProps<typeof Checkbox> & {
  header: React.ReactNode,
  checked?: boolean,
  indetermined?: boolean,
  selectable?: boolean,
  expandable?: boolean,
  expanded?: boolean,
  stayExpanded?: boolean,
  subheader?: React.ReactNode,
  children?: React.ReactNode,
  padded?: boolean,
  readOnly?: boolean,
  onChange?: (e: React.MouseEvent<Checkbox>) => void,
};

type State = {
  expanded: boolean,
};

/**
 * The ExpandableListItem is needed to render a ExpandableList.
 */
class ExpandableListItem extends React.Component<Props, State> {
  private _checkbox: Checkbox | undefined;

  static propTypes = {
    /** Is the Item checked */
    checked: PropTypes.bool,
    /**
     * Indicates whether the checkbox on this item should be in an indetermined state or not.
     * This is mostly helpful to represent cases where the element is only partially checked,
     * for instance when ExpandableListItem's child is an ExpandableList and some of its items
     * are checked, but others are not.
     */
    indetermined: PropTypes.bool,
    /** Is the item selectable */
    selectable: PropTypes.bool,
    /** Is the Item expandable */
    expandable: PropTypes.bool,
    /** Is the Item expanded */
    expanded: PropTypes.bool,
    /** Forces to stay expanded regardless of clicking on the arrow */
    stayExpanded: PropTypes.bool,
    /** The header of the item */
    header: PropTypes.oneOfType([PropTypes.string, PropTypes.element]).isRequired,
    /** The possible subheader of the item */
    subheader: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
    /** Can be a html tag or again a ExpandableList */
    children: PropTypes.oneOfType([
      PropTypes.element,
      PropTypes.arrayOf(PropTypes.element),
    ]),
    /** Leave space before and after list item */
    padded: PropTypes.bool,
    /** Mark checkbox as read only */
    readOnly: PropTypes.bool,
    /** onChange handler for the checkbox */
    onChange: PropTypes.func,
  };

  static defaultProps = {
    checked: false,
    indetermined: false,
    expandable: true,
    expanded: false,
    selectable: true,
    children: [],
    subheader: undefined,
    stayExpanded: false,
    padded: true,
    readOnly: false,
    onChange: () => undefined,
  };

  constructor(props) {
    super(props);

    this.state = {
      expanded: props.expanded,
    };
  }

  componentDidMount() {
    const { indetermined } = this.props;

    if (indetermined && this._checkbox) {
      this._checkbox.indeterminate = indetermined;
    }
  }

  componentDidUpdate(prevProps) {
    const { expanded, indetermined } = this.props;

    if (prevProps.expanded !== expanded) {
      this._toggleExpand();
    }

    if (this._checkbox) {
      this._checkbox.indeterminate = indetermined;
    }
  }

  _toggleExpand = () => {
    const { stayExpanded } = this.props;
    const { expanded } = this.state;

    if (stayExpanded) {
      this.setState({ expanded: true });
    } else {
      this.setState({ expanded: !expanded });
    }
  };

  _clickOnHeader = () => {
    if (this._checkbox) {
      this._checkbox.click();
    }
  };

  render() {
    const { expanded } = this.state;
    const { padded } = this.props;
    const { checked, expandable, selectable, header, subheader, children, ...otherProps } = this.props;
    const headerToRender = selectable ? (<Header type="button" tabIndex={0} onClick={this._clickOnHeader}>{header}</Header>) : header;
    const inputProps = _filterInputProps(otherProps);

    return (
      <ItemWrap padded={padded}>
        <Container>
          {selectable && <Checkbox inputRef={(ref) => { this._checkbox = ref; }} inline checked={checked} {...inputProps} />}
          {expandable
          && (
            <Toggle>
              <IconStack className="fa-stack" tabIndex={0} onClick={this._toggleExpand}>
                <Icon name="circle" className="fa-stack-1x" />
                <Icon name={`angle-${expanded ? 'down' : 'up'}`} className="fa-stack-1x" />
              </IconStack>
            </Toggle>
          )}
          <HeaderWrap className="header">
            {headerToRender}
            {subheader && <Subheader>{subheader}</Subheader>}
          </HeaderWrap>
        </Container>

        <ExpandableContent>
          {expanded && children}
        </ExpandableContent>
      </ItemWrap>
    );
  }
}

export default ExpandableListItem;
