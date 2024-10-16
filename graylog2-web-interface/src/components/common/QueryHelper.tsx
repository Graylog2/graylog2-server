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

import { OverlayTrigger, Icon } from 'components/common';
import { Table, Button } from 'components/bootstrap';

const COMMON_FIELD_MAP = {
  id: (entityName: string) => `Id of the ${entityName}, which is a unique reference.`,
  title: (entityName: string) => `Title of the ${entityName}.`,
  name: (entityName: string) => `Name of the ${entityName}.`,
  description: (entityName: string) => `Short description of the ${entityName}.`,
  summary: (entityName: string) => `Long summary of the ${entityName}.`,
};

type CommonFields = keyof typeof COMMON_FIELD_MAP;

type Props = {
  commonFields?: Array<CommonFields>,
  fieldMap?: { [field: string]: string },
  example?: React.ReactNode,
  entityName?: string,
};

const QueryHelpButton = styled(Button)`
  padding: 6px 8px;
`;

const row = (field: CommonFields, description: string) => (
  <tr key={`row-field-${field}`}>
    <td>{field}</td>
    <td>{description}</td>
  </tr>
);

const defaultExample = (entityName: string) => (
  <>
    <p>
      Find all {entityName}s with a description containing security:<br />
      <code>description:security</code><br />
    </p>
    <p>
      Find a {entityName} with the id 5f4dfb9c69be46153b9a9a7b:<br />
      <code>id:5f4dfb9c69be46153b9a9a7b</code><br />
    </p>
  </>
);

const queryHelpPopover = (commonFields: Props['commonFields'], fieldMap: Props['fieldMap'], example: Props['example'], entityName: Props['entityName']) => (
  <>
    <p><strong>Available search fields</strong></p>
    <Table condensed>
      <thead>
        <tr>
          <th>Field</th>
          <th>Description</th>
        </tr>
      </thead>
      <tbody>
        {commonFields.map((field) => row(field, COMMON_FIELD_MAP[field](entityName)))}
        {Object.keys(fieldMap).map((field: CommonFields) => row(field, fieldMap[field]))}
      </tbody>
    </Table>
    <p><strong>Examples</strong></p>
    {example || defaultExample(entityName)}
  </>
);

const QueryHelper = ({ commonFields = ['id', 'title', 'description'], fieldMap = {}, example, entityName = 'entity' }: Props) => (
  <OverlayTrigger trigger="click" rootClose placement="right" overlay={queryHelpPopover(commonFields, fieldMap, example, entityName)} title="Search Syntax Help" width={500}>
    <QueryHelpButton bsStyle="link"><Icon name="help" /></QueryHelpButton>
  </OverlayTrigger>
);

export default QueryHelper;
