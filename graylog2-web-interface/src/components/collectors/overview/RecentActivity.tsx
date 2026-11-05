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
import styled, { css } from 'styled-components';
import { useTranslation } from 'react-i18next';

import { Spinner, NoEntitiesExist } from 'components/common';

import ActivityEntryList from '../common/ActivityEntryList';
import { useRecentActivity } from '../hooks';

const SectionTitle = styled.h3(
  ({ theme }) => css`
    margin: 0;
    font-size: ${theme.fonts.size.h3};
  `,
);

const SectionHeader = styled.div(
  ({ theme }) => css`
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: ${theme.spacings.sm};
    margin-top: ${theme.spacings.lg};
  `,
);

const RecentActivity = () => {
  const { t } = useTranslation('collectors');
  const { data, isLoading } = useRecentActivity();

  return (
    <div>
      <SectionHeader>
        <SectionTitle>{t('recent_activity.title')}</SectionTitle>
      </SectionHeader>

      {isLoading && <Spinner />}

      {!isLoading && (!data?.activities || data.activities.length === 0) && (
        <NoEntitiesExist>{t('recent_activity.empty')}</NoEntitiesExist>
      )}

      {!isLoading && data?.activities && data.activities.length > 0 && <ActivityEntryList entries={data.activities} />}
    </div>
  );
};

export default RecentActivity;
