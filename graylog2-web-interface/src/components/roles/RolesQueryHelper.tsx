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
import { useTranslation } from 'react-i18next';

import QueryHelper from 'components/common/QueryHelper';

const EXAMPLE_QUERY = 'name:manager';
const RolesQueryHelper = () => {
  const { t } = useTranslation('users');
  const queryExample = (
    <p>
      {t('roles.query_example')}
      <br />
      <kbd>{EXAMPLE_QUERY}</kbd>
      <br />
    </p>
  );

  return <QueryHelper entityName="role" example={queryExample} commonFields={['name', 'description']} />;
};

export default RolesQueryHelper;
