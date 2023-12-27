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
import React, { useMemo } from 'react';
import slice from 'lodash/slice';

import type { IndexSetFieldTypeProfile } from 'components/indices/IndexSetFiledTypeProfiles/types';
import type { FieldTypes } from 'views/logic/fieldactions/ChangeFieldType/types';
import CustomFieldTypesList from 'components/indices/IndexSetFiledTypeProfiles/CustomFieldTypesList';
import { CUSTOM_FIELD_TYPE_LIST_SIZE } from 'components/indices/IndexSetFiledTypeProfiles/Constants';

type Props = {
  customFieldMappings: IndexSetFieldTypeProfile['customFieldMappings'],
  fieldTypes: FieldTypes,
  profileId: string,
}

const ExpandedCustomFieldTypes = ({ customFieldMappings, fieldTypes, profileId }: Props) => {
  const list = useMemo(() => slice(customFieldMappings, CUSTOM_FIELD_TYPE_LIST_SIZE - 1), [customFieldMappings]);

  // return <CustomFieldMappingsTable customFieldMappings={customFieldMappings} profileId={profileId} fieldTypes={fieldTypes} />;

  return <CustomFieldTypesList list={list} fieldTypes={fieldTypes} />;
};

export default ExpandedCustomFieldTypes;
