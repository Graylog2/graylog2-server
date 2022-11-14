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
import styled from 'styled-components';

import type { Attribute, CustomHeaders } from './ConfigurableDataTable';

const ActionsHead = styled.th`
  text-align: right;
`;

const TableHead = ({
  selectedAttributes,
  customHeaders,
  displayActionsCol,
}: {
  selectedAttributes: Array<Attribute>,
  customHeaders: CustomHeaders,
  displayActionsCol: boolean
}) => (
  <thead>
    <tr>
      {selectedAttributes.map((attribute) => {
        const headerKey = attribute.title;

        return (
          customHeaders?.[attribute.id]
            ? customHeaders[attribute.id](attribute)
            : <th key={headerKey}>{attribute.title}</th>
        );
      })}
      {displayActionsCol ? <ActionsHead>Actions</ActionsHead> : null}
    </tr>
  </thead>
);

export default TableHead;
