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
import styled from 'styled-components';

import SectionComponent from 'components/common/Section/SectionComponent';
import ContentStreamContainer from 'components/content-stream/ContentStreamContainer';
import { hasAdminPermission } from 'util/PermissionsMixin';
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
import useCurrentUser from '../../hooks/useCurrentUser';

const StyledSectionComponent = styled(SectionComponent)`
  flex-grow: 1;
`;

const Welcome = () => {
  const { permissions } = useCurrentUser();
  const isAdmin = hasAdminPermission(permissions);
  const onboardingEnabled = useFeature('onboarding_experience');
  const { metricsEnabled } = useWelcomePageConfig();

  return (
    <>
      <VisuallyHidden>
        <h1>Welcome to Graylog!</h1>
      </VisuallyHidden>
      {onboardingEnabled && <OnboardingBanner />}
      {metricsEnabled && <WelcomeMetricsSection />}
      <SectionHeader>
        <h2>Search and Usage</h2>
      </SectionHeader>
      <SectionGrid $columns="1fr 1fr 1fr">
        <StyledSectionComponent title="Favorite Items" titleAs="h3">
          <p className="description">Overview of your favorite saved searches and dashboards.</p>
          <FavoriteItemsList />
        </StyledSectionComponent>
        <StyledSectionComponent title="Recent Activity" titleAs="h3">
          <p className="description">
            {isAdmin
              ? 'This list includes all actions users performed, like creating or sharing an entity.'
              : 'Overview of actions you made with entities or somebody else made with entities which relates to you, like creating or sharing an entity.'}
          </p>
          <RecentActivityList />
        </StyledSectionComponent>
        <StyledSectionComponent title="Last Opened" titleAs="h3">
          <p className="description">Overview of recently visited saved searches and dashboards.</p>
          <LastOpenList />
        </StyledSectionComponent>
      </SectionGrid>
      <ContentStreamContainer />
    </>
  );
};

export default Welcome;
