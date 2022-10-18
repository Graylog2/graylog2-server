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
import styled from 'styled-components';

import { Icon } from 'components/common';
import FieldType from 'views/logic/fieldtypes/FieldType';

import styles from './FieldTypeIcon.css';

const iconClass = (type: string) => {
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
    case 'ip':
      return 'network-wired';
    default:
      return 'question-circle';
  }
};

const IconWrapper = styled.div`
  display: inline-flex;
  min-width: 20px;
  justify-content: center;
  align-items: center;
  vertical-align: -2px;
`;

type Props = {
  type: FieldType,
  monospace: boolean,
};

const FieldTypeIcon = ({ type, monospace }: Props) => {
  const icon = <Icon name={iconClass(type.type)} className={styles.fieldTypeIcon} />;

  return monospace ? <IconWrapper>{icon}</IconWrapper> : icon;
};

FieldTypeIcon.propTypes = {
  type: PropTypes.instanceOf(FieldType).isRequired,
  monospace: PropTypes.bool,
};

FieldTypeIcon.defaultProps = {
  monospace: true,
};

export default FieldTypeIcon;
