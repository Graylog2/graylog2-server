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

import { Icon } from 'components/common';
import FieldType from 'views/logic/fieldtypes/FieldType';

import styles from './FieldTypeIcon.css';

const iconClass = (type) => {
  switch (type) {
    case 'string':
      return 'font';
    case 'boolean':
      return 'toggle-on';
    case 'byte':
    case 'double':
    case 'float':
    case 'int':
    case 'long':
    case 'short':
      return 'chart-line';
    case 'date':
      return 'calendar-alt';
    default:
      return 'question-circle';
  }
};

const FieldTypeIcon = ({ type }) => {
  return <Icon name={iconClass(type.type)} className={styles.fieldTypeIcon} />;
};

FieldTypeIcon.propTypes = {
  type: PropTypes.instanceOf(FieldType).isRequired,
};

export default FieldTypeIcon;
