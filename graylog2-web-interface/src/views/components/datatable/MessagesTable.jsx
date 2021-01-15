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
import chroma from 'chroma-js';

import { Table } from 'components/graylog';

const MessagesContainer = styled.div`
  padding-right: 13px;
  width: 100%;
`;

const StyledTable = styled(Table)(({ theme }) => css`
  position: relative;
  font-size: ${theme.fonts.size.small};
  margin-top: 15px;
  margin-bottom: 60px;
  border-collapse: collapse;
  padding-left: 13px;
  width: 100%;
  word-break: break-all;

  thead > tr {
    color: ${theme.colors.global.textAlt};
  }

  td,
  th {
    position: relative;
    left: 13px;
  }

  > thead th {
    border: 0;
    font-size: ${theme.fonts.size.small};
    font-weight: normal;
    background-color: ${theme.colors.gray[90]};
    color: ${theme.utils.readableColor(theme.colors.gray[90])};
    white-space: nowrap;
  }
  
  > tbody td {
    background-color: ${theme.colors.global.contentBackground};
    color: ${theme.utils.contrastingColor(theme.colors.global.contentBackground)};
  }

  tr {
    border: 0 !important;
  }

  tbody.message-group {
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

const MessagesTable = ({ children }) => {
  return (
    <MessagesContainer>
      <StyledTable condensed>
        {children}
      </StyledTable>
    </MessagesContainer>
  );
};

MessagesTable.propTypes = {
  children: PropTypes.node.isRequired,
};

export default MessagesTable;
