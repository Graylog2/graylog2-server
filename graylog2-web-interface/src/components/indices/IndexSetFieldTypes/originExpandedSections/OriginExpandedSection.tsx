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

import type { FieldTypeOrigin, ExpandedSectionProps } from 'components/indices/IndexSetFieldTypes/types';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import OverriddenIndexExpandedSection
  from 'components/indices/IndexSetFieldTypes/originExpandedSections/OverriddenIndexExpandedSection';
import OverriddenProfileExpandedSection
  from 'components/indices/IndexSetFieldTypes/originExpandedSections/OverriddenProfileExpandedSection';
import ProfileExpandedSection
  from 'components/indices/IndexSetFieldTypes/originExpandedSections/ProfileExpandedSection';
import IndexExpandedSection from 'components/indices/IndexSetFieldTypes/originExpandedSections/IndexExpandedSection';

type Props = {
  origin: FieldTypeOrigin,
  type: string,
  fieldName: string,
}

const components: Record<FieldTypeOrigin, React.FC<ExpandedSectionProps>> = {
  OVERRIDDEN_INDEX: OverriddenIndexExpandedSection,
  OVERRIDDEN_PROFILE: OverriddenProfileExpandedSection,
  PROFILE: ProfileExpandedSection,
  INDEX: IndexExpandedSection,
};

const OriginExpandedSection = ({ origin, type, fieldName }: Props) => {
  const { data: { fieldTypes } } = useFieldTypesForMappings();
  const Component = components[origin];

  return <Component type={fieldTypes?.[type]} fieldName={fieldName} />;
};

export default OriginExpandedSection;
