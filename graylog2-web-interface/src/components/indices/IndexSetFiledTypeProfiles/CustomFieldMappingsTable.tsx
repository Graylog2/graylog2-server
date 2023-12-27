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

import {
  NoSearchResult,
  PaginatedDataTable,
} from 'components/common';
import type { CustomFieldMapping, IndexSetFieldTypeProfile } from 'components/indices/IndexSetFiledTypeProfiles/types';
import type { FieldTypes } from 'views/logic/fieldactions/ChangeFieldType/types';

type Props = {
  customFieldMappings: IndexSetFieldTypeProfile['customFieldMappings'],
  fieldTypes: FieldTypes,
  profileId: string,
}

const DataRowFormatter = ({ customFieldMapping, fieldTypes }: { customFieldMapping: CustomFieldMapping, fieldTypes: FieldTypes }) => (
  <tr>
    <td>{customFieldMapping.field}</td>
    <td>{fieldTypes[customFieldMapping.type]}</td>
  </tr>
);

const CustomFieldMappingsTable = ({ customFieldMappings, fieldTypes, profileId }: Props) => {
  const dataRowFormatter = (customFieldMapping: CustomFieldMapping) => (
    <DataRowFormatter customFieldMapping={customFieldMapping} fieldTypes={fieldTypes} />
  );

  return (
    <div>
      <PaginatedDataTable id={`custom-field-mappings-${profileId}`}
                          headers={['Field Name', 'Type']}
                          filterBy="field"
                          rows={customFieldMappings}
                          noDataText={<NoSearchResult>No custom mappings have been found.</NoSearchResult>}
                          dataRowFormatter={dataRowFormatter}
                          filterKeys={['field']}
                          filterLabel="Filtrate fields fields" />
    </div>
  );
};

export default CustomFieldMappingsTable;
