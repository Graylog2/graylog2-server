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

import { Spinner, Icon } from 'components/common';
import EditableTitle from 'views/components/common/EditableTitle';

import styles from './WidgetHeader.css';

import CustomPropTypes from '../CustomPropTypes';

const LoadingSpinner = styled(Spinner)`
  margin-left: 10px;
`;

const WidgetHeader = ({ children, onRename, hideDragHandle, title, loading }) => (
  <div className={styles.widgetHeader}>
    {hideDragHandle || <Icon name="bars" className={`widget-drag-handle ${styles.widgetDragHandle}`} />}{' '}
    <EditableTitle key={title} disabled={!onRename} value={title} onChange={onRename} />
    {loading && <LoadingSpinner text="" delay={0} />}
    <span className={`pull-right ${styles.widgetActionDropdown}`}>
      {children}
    </span>
  </div>
);

WidgetHeader.propTypes = {
  children: CustomPropTypes.OneOrMoreChildren,
  onRename: PropTypes.func,
  hideDragHandle: PropTypes.bool,
  title: PropTypes.node.isRequired,
  loading: PropTypes.bool,
};

WidgetHeader.defaultProps = {
  children: null,
  onRename: undefined,
  hideDragHandle: false,
  loading: false,
};

export default WidgetHeader;
