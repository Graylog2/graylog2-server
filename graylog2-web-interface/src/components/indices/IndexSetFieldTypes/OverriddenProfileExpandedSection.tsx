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
import useFieldTypes from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes';
import { useStore } from 'stores/connect';
import { IndexSetsStore } from 'stores/indices/IndexSetsStore';

type Props = {
  type: string,
  fieldName: string,
}

const OverriddenProfileExpandedSection = ({ type, fieldName }: Props) => {
  const { indexSet: { title: indexSetTitle, id } } = useStore(IndexSetsStore);
  const { data: { fieldTypes } } = useFieldTypes();
  const profileFieldType = 'String (aggregatable)';
  const profileName = 'My Profile Name';

  return (
    <div>
      Field type <i>{profileFieldType}</i> for <b>{fieldName}</b> in
      profile <Link to={Routes.SYSTEM.INDICES.LIST}>{profileName}</Link> is overridden with a
      field type <i>{fieldTypes[type]}</i> in <Link to={Routes.SYSTEM.INDEX_SETS.SHOW(id)}>{indexSetTitle}</Link>
    </div>
  );
};

export default OverriddenProfileExpandedSection;
