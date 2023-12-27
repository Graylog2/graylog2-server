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
import take from 'lodash/take';

import type { CustomFieldMapping, IndexSetFieldTypeProfile } from 'components/indices/IndexSetFiledTypeProfiles/types';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';
import type { FieldTypes } from 'views/logic/fieldactions/ChangeFieldType/types';
import { CUSTOM_FIELD_TYPE_LIST_SIZE } from 'components/indices/IndexSetFiledTypeProfiles/Constants';
import CustomFieldTypesList from 'components/indices/IndexSetFiledTypeProfiles/CustomFieldTypesList';
import { Button } from 'components/bootstrap';

const CustomFieldMappingsCell = ({ customFieldTypes, profile, fieldTypes }: { customFieldTypes : Array<CustomFieldMapping>, profile: IndexSetFieldTypeProfile, fieldTypes: FieldTypes }) => {
  const { toggleSection } = useExpandedSections();
  const customFieldTypesToShow = take(customFieldTypes, CUSTOM_FIELD_TYPE_LIST_SIZE);
  const restItemsQuantity = customFieldTypes.length - customFieldTypesToShow.length;

  return (
    <>
      <CustomFieldTypesList list={customFieldTypesToShow} fieldTypes={fieldTypes} />
      {restItemsQuantity > 0 && <Button bsStyle="link" onClick={() => toggleSection(profile.id, 'customFieldMapping')}>...and {restItemsQuantity} more</Button>}
    </>
  );
};

export default CustomFieldMappingsCell;
