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

import { OverlayTrigger, Popover, Table, Button } from 'components/graylog';
import { Icon } from 'components/common';

const COMMON_FIELD_MAP = {
  id: (entityName) => `Id of the ${entityName}, which is a unique reference.`,
  title: (entityName) => `Title of the ${entityName}.`,
  name: (entityName) => `Name of the ${entityName}.`,
  description: (entityName) => `Short description of the ${entityName}.`,
  summary: (entityName) => `Long summary of the ${entityName}.`,
};

type CommonFields = keyof typeof COMMON_FIELD_MAP;

type Props = {
  commonFields?: Array<CommonFields>,
  fieldMap?: { [field: string]: string },
  example?: string,
  entityName?: string,
};

const row = (field, description) => (
  <tr>
    <td>{field}</td>
    <td>{description}</td>
  </tr>
);

const defaultExample = (
  <>
    <p>
      Find entities with a description containing security:<br />
      <code>description:security</code><br />
    </p>
    <p>
      Find a entities with the id 5f4dfb9c69be46153b9a9a7b:<br />
      <code>id:5f4dfb9c69be46153b9a9a7b</code><br />
    </p>
  </>
);

const queryHelpPopover = (commonFields, fieldMap, example, entityName) => (
  <Popover id="team-search-query-help" title="Search Syntax Help">
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
        {Object.keys(fieldMap).map((field) => row(field, fieldMap[field]))}
      </tbody>
    </Table>
    <p><strong>Examples</strong></p>
    {example || defaultExample}
  </Popover>
);

const QueryHelper = ({ commonFields, fieldMap, example, entityName }: Props) => (
  <OverlayTrigger trigger="click" rootClose placement="right" overlay={queryHelpPopover(commonFields, fieldMap, example, entityName)}>
    <Button bsStyle="link"><Icon name="question-circle" /></Button>
  </OverlayTrigger>
);

QueryHelper.defaultProps = {
  commonFields: ['id', 'title', 'description'],
  fieldMap: {},
  example: undefined,
  entityName: 'entity',
};

export default QueryHelper;
