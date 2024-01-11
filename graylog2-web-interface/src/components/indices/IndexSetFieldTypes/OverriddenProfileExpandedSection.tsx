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

import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import { useStore } from 'stores/connect';
import { IndexSetsStore } from 'stores/indices/IndexSetsStore';
import useProfileWithMappingsByField from 'components/indices/IndexSetFieldTypes/hooks/useProfileWithMappingsByField';

type Props = {
  type: string,
  fieldName: string,
}

const OverriddenProfileExpandedSection = ({ type, fieldName }: Props) => {
  const { indexSet: { title: indexSetTitle, id, field_type_profile } } = useStore(IndexSetsStore);
  const { data: { fieldTypes } } = useFieldTypesForMappings();
  const { customFieldMappingsByField, name: profileName } = useProfileWithMappingsByField(field_type_profile);
  const profileFieldType = customFieldMappingsByField?.[fieldName];

  return (
    <div>
      Field type <i>{profileFieldType}</i> for <b>{fieldName}</b> in
      profile <Link to={Routes.SYSTEM.INDICES.LIST}>{profileName}</Link> is overridden with a
      field type <i>{fieldTypes[type]}</i> in <Link to={Routes.SYSTEM.INDEX_SETS.SHOW(id)}>{indexSetTitle}</Link>
    </div>
  );
};

export default OverriddenProfileExpandedSection;
