import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Table } from 'components/graylog';

const MessagesContainer = styled.div`
  padding-right: 13px;
  width: 100%;
`;

const StyledTable = styled(Table)`
  position: relative;
  font-size: 11px;
  margin-top: 15px;
  margin-bottom: 60px;
  border-collapse: collapse;
  padding-left: 13px;
  width: 100%;
  word-break: break-all;

  thead > tr {
    color: #fff;
  }

  td,
  th {
    position: relative;
    left: 13px;
  }

  > thead th {
    border: 0;
    font-size: 11px;
    font-weight: normal;
    background-color: #222;
    white-space: nowrap;
  }

  tr {
    border: 0 !important;
  }

  tbody.message-group {
    border-top: 0;
  }

  tbody.message-group-toggled {
    border-left: 7px solid #16ace3;
  }

  tbody.message-highlight {
    border-left: 7px solid #8dc63f;
  }

  tr.fields-row {
    cursor: pointer;

    td {
      padding-top: 10px;
    }
  }

  tr.message-row td {
    border-top: 0;
    padding-top: 0;
    padding-bottom: 5px;
    font-family: monospace;
    color: #16ace3;
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
        background: linear-gradient(to bottom, rgba(255, 255, 255, 0), rgba(255, 255, 255, 1) 95%);
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
    color: #222; /* same color as .messsages th cannot display: none it because it would push the table columns out on hover :( */
  }

  th i.sort-order-active,
  th:hover i.sort-order-item {
    color: #fff;
  }
`;

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
