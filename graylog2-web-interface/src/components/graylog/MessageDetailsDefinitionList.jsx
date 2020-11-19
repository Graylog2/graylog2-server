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
import styled, { css } from 'styled-components';

const MessageDetailsDefinitionList = styled.dl(({ theme }) => css`
  margin-top: 10px;
  margin-bottom: 0;

  dt {
    font-weight: bold;
    margin-left: 1px;
  }

  dd {
    margin-bottom: 5px;
    padding-bottom: 5px;
    margin-left: 1px; /* Ensures that italic text is not cut */

    &.stream-list ul {
      list-style-type: disc;
      padding-left: 25px;

      li {
        margin-top: 3px;
      }
    }

    div.message-field-actions {
      padding-left: 10px;
      position: relative;
      top: -10px;
    }
  }

  &.message-details-fields span:not(:last-child) dd {
    border-bottom: 1px solid ${theme.colors.gray[90]};
  }

  &.message-details-fields dd {
    white-space: pre-wrap;
  }

  &.message-details-fields .field-value {
    font-family: ${theme.fonts.family.monospace};
  }

  &.message-details-fields dd.message-field .field-value {
    max-height: 500px;
    overflow: auto;
  }
`);

export default MessageDetailsDefinitionList;
