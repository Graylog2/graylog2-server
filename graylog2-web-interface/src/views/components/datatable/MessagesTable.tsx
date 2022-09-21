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
import PropTypes from 'prop-types';
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

import { Table } from 'components/bootstrap';

const MessagesContainer = styled.div`
  width: 100%;
`;

const StyledTable = styled(Table)(({ theme, $stickyHeader, $borderedHeader }: { theme: DefaultTheme, $stickyHeader: boolean, $borderedHeader: boolean }) => css`
  position: relative;
  font-size: ${theme.fonts.size.small};
  margin: 0;
  border-collapse: collapse;
  width: 100%;
  word-break: break-all;
  
  thead {
  ${$stickyHeader
    ? `position: sticky;
    top: 0;
    z-index: 2` : ''}
  }
  
  thead > tr {
    color: ${theme.colors.global.textAlt};
  }
  
  td,
  th {
    position: relative;
  }

  > thead th {
    border: 0;
    font-size: ${theme.fonts.size.small};
    font-weight: normal;
    background-color: ${theme.colors.gray[90]};
    color: ${theme.utils.readableColor(theme.colors.gray[90])};
    white-space: nowrap;
    ${
  $borderedHeader ? `
            border: 1px solid ${theme.colors.table.backgroundAlt}
        ` : ''
}
  }

  > tbody td {
    background-color: ${theme.colors.global.contentBackground};
    color: ${theme.utils.contrastingColor(theme.colors.global.contentBackground)};
  }

  &.table-striped>tbody>tr:nth-of-type(odd)>td {
    background-color: ${theme.colors.global.contentBackground};
  }
  &.table-striped>tbody>tr:nth-of-type(even)>td {
    background-color: ${theme.colors.table.background};
  }
  tr {
    border: 0 !important;
  }

  tr.message-group {
    border-top: 0;
  }

  tbody.message-group-toggled {
    border-left: 7px solid ${theme.colors.variant.light.info};
  }

  tbody.message-highlight {
    border-left: 7px solid ${theme.colors.variant.light.success};
  }

  tr.fields-row {
    cursor: pointer;

    td {
      min-width: 50px;
      padding-top: 10px;
    }
  }

  tr.message-row td {
    border-top: 0;
    padding-top: 0;
    padding-bottom: 5px;
    font-family: ${theme.fonts.family.monospace};
    color: ${theme.colors.variant.dark.info};
  }

  tr.message-row {
    margin-bottom: 5px;
    cursor: pointer;

    .message-wrapper {
      line-height: 1.5em;
      white-space: pre-line;
      max-height: 6em; /* show 4 lines: line-height * 4 */
      overflow: hidden;

      &::after {
        content: "";
        text-align: right;
        position: absolute;
        width: 99%;
        left: 5px;
        top: 4.5em;
        height: 1.5em;
        background: linear-gradient(to bottom, ${chroma(theme.colors.global.contentBackground).alpha(0).css()}, ${chroma(theme.colors.global.contentBackground).alpha(1).css()} 95%);
      }
    }
  }

  tr.message-detail-row {
    display: none;
  }

  tr.message-detail-row td {
    padding-top: 5px;
    border-top: 0;
  }

  tr.message-detail-row .row {
    margin-right: 0;
  }

  tr.message-detail-row div[class*="col-"] {
    padding-right: 0;
  }

  th i.sort-order-desc {
    position: relative;
    top: -1px;
  }

  th i.sort-order-item {
    margin-right: 2px;
    color: ${theme.colors.gray[10]};
    visibility: hidden;
  }

  th i.sort-order-active,
  th:hover i.sort-order-item {
    color: ${theme.colors.global.textAlt};
  }
`);

type Props = {
  children: React.ReactNode,
  striped?: boolean,
  bordered?: boolean,
  borderedHeader?: boolean,
  stickyHeader?: boolean,
  condensed?: boolean,
};

const MessagesTable = ({ children, condensed, striped, bordered, stickyHeader, borderedHeader }: Props) => {
  return (
    <MessagesContainer>
      <StyledTable condensed={condensed}
                   striped={striped}
                   bordered={bordered}
                   $stickyHeader={stickyHeader}
                   $borderedHeader={borderedHeader}>
        {children}
      </StyledTable>
    </MessagesContainer>
  );
};

MessagesTable.propTypes = {
  children: PropTypes.node.isRequired,
  condensed: PropTypes.bool,
  striped: PropTypes.bool,
  bordered: PropTypes.bool,
  stickyHeader: PropTypes.bool,
  borderedHeader: PropTypes.bool,
};

MessagesTable.defaultProps = {
  condensed: true,
  striped: false,
  bordered: false,
  stickyHeader: false,
  borderedHeader: false,
};

export default MessagesTable;
