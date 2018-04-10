import React from 'react';
import PropTypes from 'prop-types';

import style from './ExpandableList.css';
/**
 * The ExpandableList will take a array or one of ExpandeableListItem to render
 * in list. This list can be expanded or flattened to give the user a overview
 * of categories. Inside the categories the user has the possibility of doing a selection.
 * The ExpandableList can be used nested.
 */
class ExpandableList extends React.Component {
  static propTypes = {
    /**
     * One or more elements of ExpandableListItem
     */
    children: PropTypes.oneOfType([
      PropTypes.element,
      PropTypes.arrayOf(PropTypes.element),
    ]),
  };

  static defaultProps = {
    children: [],
  };

  render() {
    const { children } = this.props;

    return (
      <ul className={style.list}>
        {children}
      </ul>
    );
  }
}

export default ExpandableList;
