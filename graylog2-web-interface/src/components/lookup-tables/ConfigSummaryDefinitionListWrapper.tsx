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
import styled from 'styled-components';

const ConfigSummaryDefinitionListWrapper = styled.div`
  width: 100%;

  dl {
    margin-top: 10px;

    dt {
      float: left;
      clear: left;
      min-width: 200px;
      min-height: 33px;
      padding: 7px 0;
    }

    dd {
      margin-left: 200px;
      word-wrap: break-word;
      width: 55%;
      min-height: 33px;
      padding: 7px 10px;

      :not(:last-child) {
        border-bottom: 1px solid ${(props) => props.theme.colors.terniary};
      }

      code {
        margin-left: -3px;
      }
    }
  }
`;

export default ConfigSummaryDefinitionListWrapper;
