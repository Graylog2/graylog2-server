import React from 'react';
import PropTypes from 'prop-types';
import { Checkbox } from 'components/graylog';
import Icon from './Icon';

import style from './ExpandableListItem.css';

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
    const { checked, expandable, selectable, header, subheader, children, ...otherProps } = this.props;
    const headerToRender = selectable ? (<span role="button" tabIndex={0} onClick={this._clickOnHeader}>{header}</span>) : header;
    const inputProps = this._filterInputProps(otherProps);
    const liClassName = this.props.padded ? style.listItemPadding : '';

    return (
      <li className={liClassName}>
        <div className={style.listItemContainer}>
          {selectable && <Checkbox inputRef={(c) => { this._checkbox = c; }} inline checked={checked} {...inputProps} />}
          {expandable
          && (
          <div className={style.expandBoxContainer}>
            <div className={`fa-stack ${style.expandBox}`} role="button" tabIndex={0} onClick={this._toggleExpand}>
              <Icon name="circle" className={`fa-stack-1x ${style.iconBackground}`} />
              <Icon name={`angle-${expanded ? 'down' : 'up'}`} className="fa-stack-1x" />
            </div>
          </div>
          )
          }
          <span className={style.header}>{headerToRender}{subheader && <span className={style.subheader}>{subheader}</span>}</span>
        </div>
        <div className={style.expandableContent}>
          {expanded && children}
        </div>
      </li>
    );
  }
}

export default ExpandableListItem;
