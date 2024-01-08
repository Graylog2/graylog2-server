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
import { styled } from 'styled-components';

import type { CustomFieldMapping, IndexSetFieldTypeProfile } from 'components/indices/IndexSetFieldTypeProfiles/types';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';
import { CountBadge } from 'components/common';

const StyledCountBadge = styled(CountBadge)`
  cursor: pointer;
`;

const CustomFieldMappingsCell = ({ customFieldTypes, profile }: { customFieldTypes : Array<CustomFieldMapping>, profile: IndexSetFieldTypeProfile }) => {
  const { toggleSection } = useExpandedSections();

  return (
    <StyledCountBadge onClick={() => toggleSection(profile.id, 'customFieldMapping')}>{customFieldTypes.length}</StyledCountBadge>
  );
};

export default CustomFieldMappingsCell;
