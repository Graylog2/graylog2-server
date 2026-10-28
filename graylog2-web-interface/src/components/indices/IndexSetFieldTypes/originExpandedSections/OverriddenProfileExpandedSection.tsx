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
import { Trans, useTranslation } from 'react-i18next';

import { Link } from 'components/common';
import Routes from 'routing/Routes';
import useIndexProfileWithMappingsByField from 'components/indices/IndexSetFieldTypes/hooks/useIndexProfileWithMappingsByField';
import type { ExpandedSectionProps } from 'components/indices/IndexSetFieldTypes/types';

const OverriddenProfileExpandedSection = ({ type, fieldName }: ExpandedSectionProps) => {
  const { t } = useTranslation('indexsets');
  const { customFieldMappingsByField, name: profileName } = useIndexProfileWithMappingsByField();
  const profileFieldType = customFieldMappingsByField?.[fieldName];

  return (
    <div>
      <Trans
        t={t}
        i18nKey="field_types.origin_overridden_profile"
        values={{ type, fieldName, profileFieldType, profileName }}
        components={{ i: <i />, b: <b />, profileLink: <Link to={Routes.SYSTEM.INDICES.LIST} /> }}
      />
    </div>
  );
};

export default OverriddenProfileExpandedSection;
