import React from 'react';
import PropTypes from 'prop-types';
import { Checkbox } from 'react-bootstrap';

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
    /** The header of the item */
    header: PropTypes.oneOfType([PropTypes.string, PropTypes.element]).isRequired,
    /** The possible subheader of the item */
    subheader: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
    /** Can be a html tag or again a ExpandableList */
    children: PropTypes.oneOfType([
      PropTypes.element,
      PropTypes.arrayOf(PropTypes.element),
    ]),
  };

  static defaultProps = {
    checked: false,
    indetermined: false,
    expandable: true,
    expanded: false,
    selectable: true,
    children: [],
    subheader: undefined,
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
    this.setState({ expanded: !this.state.expanded });
  };

  _filterInputProps = (props) => {
    const { expanded, indetermined, ...inputProps } = props;
    return inputProps;
  };

  render() {
    const { expanded } = this.state;
    const { checked, expandable, selectable, header, subheader, children, ...otherProps } = this.props;
    const inputProps = this._filterInputProps(otherProps);

    return (
      <li className={style.listItem}>
        <div className={style.listItemContainer}>
          {selectable && <Checkbox inputRef={(c) => { this._checkbox = c; }} inline checked={checked} {...inputProps} />}
          {expandable &&
          <div className={style.expandBoxContainer}>
            <div className={`fa-stack ${style.expandBox}`} role="button" tabIndex={0} onClick={this._toggleExpand}>
              <i className={`fa fa-circle-thin fa-stack-1x ${style.iconBackground}`} />
              <i className={`fa fa-stack-1x fa-angle-${expanded ? 'down' : 'up'}`} />
            </div>
          </div>
          }
          <span className={style.header}>{header}{subheader && <span className={style.subheader}>{subheader}</span>}</span>
        </div>
        <div className={style.expandableContent}>
          {expanded && children}
        </div>
      </li>
    );
  }
}

export default ExpandableListItem;
