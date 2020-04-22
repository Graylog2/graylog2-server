import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Checkbox } from 'components/graylog';
import Icon from './Icon';

const ItemWrap = styled.li(({ padded }) => `
  padding: ${padded ? '10px 5px' : ''};
`);

const Container = styled.div`
  font-size: 14px;
  line-height: 20px;

  label {
    min-height: 20px;
    margin-bottom: 2px;
    margin-right: 5px;

    * {
      cursor: pointer;
    }
  }
`;

const Toggle = styled.div`
  display: inline-block;
  width: 20px;
  margin-right: 5px;
`;

const IconStack = styled.div`
  &.fa-stack {
    cursor: pointer;
    font-size: 1.4em;
    line-height: 20px;
    width: 1em;
    height: 1em;
    vertical-align: text-top;

    &:hover [class*="fa-"] {
      color: #731748;
      opacity: 1;
    }
  }

  [class*="fa-"]:first-child {
    opacity: 0;

    ~ [class*="fa-"]:hover {
      color: #fff;
    }
  }
`;

const HeaderWrap = styled.span`
  font-size: 14px;
`;

const Subheader = styled.span`
  font-size: 0.95em;
  margin-left: 0.5em;
  color: #aaa;
`;

const ExpandableContent = styled.div`
  border-left: 1px #eee solid;
  margin-left: 35px;
  margin-top: 10px;
  padding-left: 5px;
`;


/**
 * The ExpandableListItem is needed to render a ExpandableList.
 */
class ExpandableListItem extends React.Component {
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
  };

  state = {
    expanded: this.props.expanded,
    selectable: false,
    subheader: '',
  };

  componentDidMount() {
    if (this.props.indetermined && this._checkbox) {
      this._checkbox.indeterminate = this.props.indetermined;
    }
  }

  componentDidUpdate(prevProps) {
    if (prevProps.expanded !== this.props.expanded) {
      this._toggleExpand();
    }

    if (this._checkbox) {
      this._checkbox.indeterminate = this.props.indetermined;
    }
  }

  _checkbox = undefined;

  _toggleExpand = () => {
    if (this.props.stayExpanded) {
      this.setState({ expanded: true });
    } else {
      this.setState({ expanded: !this.state.expanded });
    }
  };

  _filterInputProps = (props) => {
    const { expanded, indetermined, stayExpanded, padded, ...inputProps } = props;
    return inputProps;
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
    const headerToRender = selectable ? (<span role="button" tabIndex={0} onClick={this._clickOnHeader}>{header}</span>) : header;
    const inputProps = this._filterInputProps(otherProps);

    return (
      <ItemWrap padded={padded}>
        <Container>
          {selectable && <Checkbox inputRef={(c) => { this._checkbox = c; }} inline checked={checked} {...inputProps} />}
          {expandable
          && (
            <Toggle>
              <IconStack className="fa-stack" tabIndex={0} onClick={this._toggleExpand}>
                <Icon name="circle" className="fa-stack-1x" />
                <Icon name={`angle-${expanded ? 'down' : 'up'}`} className="fa-stack-1x" />
              </IconStack>
            </Toggle>
          )}
          <HeaderWrap>
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
