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
import * as React from 'react';
import styled, { css } from 'styled-components';

const WidgetWrap = styled.div(({ theme }) => css`
  height: inherit;
  margin: 0;
  padding: 7px 9px 6px 9px;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .dc-chart {
    float: none;
  }

  .controls {
    display: none;
    position: relative;
    left: -3px;
  }

  .reloading {
    margin-right: 2px;
    font-weight: bold;
    color: ${theme.colors.variant.dark.info};
    display: none;
  }

  .loading-failed {
    color: ${theme.colors.variant.danger} !important;
  }

  .widget-title {
    font-size: ${theme.fonts.size.large};
    height: 25px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .load-error {
    color: ${theme.colors.variant.danger};
    margin-right: 5px;
  }

  .widget-update-info {
    text-align: left;
    float: left;
    font-size: ${theme.fonts.size.small};
    position: absolute;
    bottom: 10px;
    width: 130px;
  }

  .configuration dt {
    text-transform: capitalize;
  }

  svg {
    overflow: hidden;
  }

  .quickvalues-graph {
    text-align: center;
  }

  .graph.scatterplot path.line {
    display: none;
  }

  .actions {
    position: absolute;
    right: 15px;
    bottom: 10px;

    div {
      display: inline-block;
      margin-left: 5px;
    }

    button {
      padding: 0 5px 0 5px;
    }
  }
`);

type Props = {
  children: React.ReactNode,
  widgetId: string
}

const WidgetFrame = ({ children, widgetId }: Props) => (
  <WidgetWrap data-widget-id={widgetId}>
    {children}
  </WidgetWrap>
);

export default WidgetFrame;
