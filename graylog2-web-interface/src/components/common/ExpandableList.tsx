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

type Props = {
  children?: React.ReactElement,
  className?: string,
};

/**
 * The ExpandableList will take a array or one of ExpandableListItem to render
 * in list. This list can be expanded or flattened to give the user a overview
 * of categories. Inside the categories the user has the possibility of doing a selection.
 * The ExpandableList can be used nested.
 */
const ExpandableList = ({ children, className }: Props) => {
  return (
    <ul className={className ? `${style.list} ${className}` : style.list}>
      {children}
    </ul>
  );
};

ExpandableList.defaultProps = {
  children: [],
  className: undefined,
};

ExpandableList.propTypes = {
  /**
   * One or more elements of ExpandableListItem
   */
  children: PropTypes.oneOfType([
    PropTypes.element,
    PropTypes.arrayOf(PropTypes.element),
  ]),
  className: PropTypes.string,
};

export default ExpandableList;
