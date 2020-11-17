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
// @flow strict
import * as React from 'react';
import { useState, useCallback } from 'react';

import SharedEntitiesOverview from 'components/permissions/SharedEntitiesOverview';
import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import User from 'logic/users/User';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  userId: $PropertyType<User, 'id'>,
};

const SharedEntitiesSection = ({ userId }: Props) => {
  const [loading, setLoading] = useState(false);
  const _searchPaginated = useCallback((pagination) => EntityShareDomain.loadUserSharesPaginated(userId, pagination), [userId]);

  return (
    <SectionComponent title="Shared Entities" showLoading={loading}>
      <SharedEntitiesOverview setLoading={setLoading}
                              entityType="user"
                              searchPaginated={_searchPaginated} />
    </SectionComponent>
  );
};

export default SharedEntitiesSection;
