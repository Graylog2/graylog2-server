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
import { useTranslation } from 'react-i18next';

import HideOnCloud from 'util/conditional/HideOnCloud';
import { Col } from 'components/bootstrap';
import { IndicesConfiguration } from 'components/indices';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import useProfile from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfile';
import { Link } from 'components/common';
import Routes from 'routing/Routes';

import StyledIndexSetDetailsRow from './StyledIndexSetDetailsRow';

type Props = {
  indexSet: IndexSet;
};

const IndexSetDetails = ({ indexSet }: Props) => {
  const { t } = useTranslation('indexsets');
  const {
    data: { name: profileName },
    isFetching,
  } = useProfile(indexSet.field_type_profile);

  return (
    <StyledIndexSetDetailsRow>
      <Col lg={3}>
        <dl>
          <dt>{t('set_details.index_prefix')}</dt>
          <dd>{indexSet.index_prefix}</dd>
          <HideOnCloud>
            <dt>{t('set_details.shards')}</dt>
            <dd>{indexSet.shards}</dd>
            <dt>{t('set_details.replicas')}</dt>
            <dd>{indexSet.replicas}</dd>
          </HideOnCloud>
          <dt>{t('set_details.field_type_refresh_interval')}</dt>
          <dd>{t('set_details.seconds', { count: indexSet.field_type_refresh_interval / 1000.0 })}</dd>
          <dt>{t('set_details.field_type_profile')}</dt>
          {!isFetching && (
            <dd>
              {indexSet.field_type_profile ? (
                <Link to={Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.edit(indexSet.field_type_profile)} target="_blank">
                  {profileName}
                </Link>
              ) : (
                <i>{t('set_details.not_set')}</i>
              )}
            </dd>
          )}
        </dl>
      </Col>

      <Col lg={6}>
        <IndicesConfiguration indexSet={indexSet} />
      </Col>
    </StyledIndexSetDetailsRow>
  );
};

export default IndexSetDetails;
