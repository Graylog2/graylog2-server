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
import styled, { css } from 'styled-components';

import { Spinner, Icon } from 'components/common';
import EditableTitle from 'views/components/common/EditableTitle';

import CustomPropTypes from '../CustomPropTypes';

const LoadingSpinner = styled(Spinner)`
  margin-left: 10px;
`;

const Container = styled.div(({ theme }) => css`
  font-size: ${theme.fonts.size.large};
  text-overflow: ellipsis;
  margin-bottom: 5px;
`);

const WidgetDragHandle = styled(Icon)`
  cursor: move;
  opacity: 0.3;
  margin-right: 5px;
`;

const WidgetActionDropdown = styled.span`
  position: relative;
  z-index: 1;
`;

const WidgetHeader = ({ children, onRename, hideDragHandle, title, loading }) => (
  <Container>
    {hideDragHandle || <WidgetDragHandle name="bars" className="widget-drag-handle" />}
    <EditableTitle key={title} disabled={!onRename} value={title} onChange={onRename} />
    {loading && <LoadingSpinner text="" delay={0} />}
    <WidgetActionDropdown className="pull-right">
      {children}
    </WidgetActionDropdown>
  </Container>
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
