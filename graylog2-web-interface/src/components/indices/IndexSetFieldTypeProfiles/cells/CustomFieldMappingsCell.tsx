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
import type { CustomFieldMapping } from 'components/indices/IndexSetFieldTypeProfiles/types';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';

type Props = {
  customFieldTypes: Array<CustomFieldMapping>;
};

const CustomFieldMappingsCell = ({ customFieldTypes }: Props) => {
  const {
    data: { fieldTypes },
  } = useFieldTypesForMappings();
  const typeTitle = (type: string) => fieldTypes[type] ?? type;

  return (
    <ExpandableCellList
      items={[...customFieldTypes].sort((a, b) => a.field.localeCompare(b.field))}
      renderItem={({ field, type }) => (
        <DefinitionList>
          <dt>{field}</dt>
          <dd>{typeTitle(type)}</dd>
        </DefinitionList>
      )}
      itemKey={({ field }) => field}
      itemTitle={({ field, type }) => `${field}: ${typeTitle(type)}`}
    />
  );
};

export default CustomFieldMappingsCell;
