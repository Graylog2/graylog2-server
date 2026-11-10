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

import { DefinitionList } from 'components/common';
import ExpandableCellList from 'components/common/ExpandableCellList';
import { defaultCompare } from 'logic/DefaultCompare';

type Props = {
  fields: Record<string, string>;
};

/**
 * Lists the fields of an event in an entity data table cell.
 */
const EventFieldsCellList = ({ fields }: Props) => (
  <ExpandableCellList
    items={Object.keys(fields).sort(defaultCompare)}
    renderItem={(fieldName) => (
      <DefinitionList>
        <dt>{fieldName}</dt>
        <dd>{fields[fieldName]}</dd>
      </DefinitionList>
    )}
    itemKey={(fieldName) => fieldName}
    itemTitle={(fieldName) => `${fieldName}: ${fields[fieldName]}`}
  />
);

export default EventFieldsCellList;
