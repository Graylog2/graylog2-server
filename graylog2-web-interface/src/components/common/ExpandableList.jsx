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
