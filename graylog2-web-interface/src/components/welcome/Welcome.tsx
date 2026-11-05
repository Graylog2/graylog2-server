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
import styled from 'styled-components';

import useProductName from 'brand-customization/useProductName';
import SectionComponent from 'components/common/Section/SectionComponent';
import ContentStreamContainer from 'components/content-stream/ContentStreamContainer';
import usePermissions from 'hooks/usePermissions';
import useFeature from 'hooks/useFeature';
import SectionHeader from 'components/welcome/SectionHeader';
import VisuallyHidden from 'components/common/VisuallyHidden';

import LastOpenList from './LastOpenList';
import FavoriteItemsList from './FavoriteItemsList';
import RecentActivityList from './RecentActivityList';
import OnboardingBanner from './OnboardingBanner';
import WelcomeMetricsSection from './WelcomeMetricsSection';
import useWelcomePageConfig from './hooks/useWelcomePageConfig';

import SectionGrid from '../common/Section/SectionGrid';

const StyledSectionComponent = styled(SectionComponent)`
  flex-grow: 1;
`;

const Welcome = () => {
  const { t } = useTranslation('welcome');
  const { isAdmin } = usePermissions();
  const onboardingEnabled = useFeature('onboarding_experience');
  const { metricsEnabled } = useWelcomePageConfig();
  const productName = useProductName();

  return (
    <>
      <VisuallyHidden>
        <h1>{t('welcome_to', { productName })}</h1>
      </VisuallyHidden>
      {onboardingEnabled && <OnboardingBanner />}
      {metricsEnabled && <WelcomeMetricsSection />}
      <SectionHeader>
        <h2>{t('search_and_usage')}</h2>
      </SectionHeader>
      <SectionGrid $columns="1fr 1fr 1fr">
        <StyledSectionComponent title={t('favorite_items')} titleAs="h3">
          <p className="description">{t('favorite_items_description')}</p>
          <FavoriteItemsList />
        </StyledSectionComponent>
        <StyledSectionComponent title={t('recent_activity')} titleAs="h3">
          <p className="description">
            {isAdmin ? t('recent_activity_description_admin') : t('recent_activity_description')}
          </p>
          <RecentActivityList />
        </StyledSectionComponent>
        <StyledSectionComponent title={t('last_opened')} titleAs="h3">
          <p className="description">{t('last_opened_description')}</p>
          <LastOpenList />
        </StyledSectionComponent>
      </SectionGrid>
      <ContentStreamContainer />
    </>
  );
};

export default Welcome;
