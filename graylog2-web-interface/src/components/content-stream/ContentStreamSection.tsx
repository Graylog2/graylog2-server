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
import styled, { css } from 'styled-components';

import SectionGrid from 'components/common/Section/SectionGrid';
import SectionComponent from 'components/common/Section/SectionComponent';
import ContentStreamNews from 'components/content-stream/ContentStreamNews';
import ContentStreamNewsFooter from 'components/content-stream/news/ContentStreamNewsFooter';
import ContentStreamReleasesSection from 'components/content-stream/ContentStreamReleasesSection';
import useContentStreamSettings from 'components/content-stream/hook/useContentStreamSettings';
import useCurrentUser from 'hooks/useCurrentUser';
import ToggleActionButton from 'components/content-stream/ToggleActionButton';

const StyledNewsSectionComponent = styled(SectionComponent)<{ $enabled: boolean }>(({ $enabled, theme }) => css`
  overflow: hidden;
  flex-grow: 3;
  height: ${$enabled ? 'initial' : 'min-content'};
  @media (max-width: ${theme.breakpoints.max.md}) {
    flex-grow: 1;
  }
`);
const StyledReleaseSectionComponent = styled(SectionComponent)<{ $enabled: boolean }>(({ $enabled }) => css`
  flex-grow: 1;
  height: ${$enabled ? 'initial' : 'min-content'};
`);

const ContentStreamSection = () => {
  const { username } = useCurrentUser();
  const {
    contentStreamSettings,
    isLoadingContentStreamSettings,
    onSaveContentStreamSetting,
    refetchContentStream,
  } = useContentStreamSettings();

  if (isLoadingContentStreamSettings || !contentStreamSettings) {
    return null;
  }

  const updateContentStreamSettings = async ({ enableContentStream, enableRelease }: {
    enableContentStream?: boolean,
    enableRelease?: boolean
  }) => {
    await onSaveContentStreamSetting({
      settings: {
        content_stream_enabled: enableContentStream,
        releases_enabled: enableRelease,
        content_stream_topics: contentStreamSettings.contentStreamTopics,
      },
      username,
    });

    refetchContentStream();
  };

  const { contentStreamEnabled, releasesSectionEnabled } = contentStreamSettings;
  const toggleNews = () => updateContentStreamSettings({
    enableContentStream: !contentStreamEnabled,
    enableRelease: releasesSectionEnabled,
  });
  const toggleRelease = () => updateContentStreamSettings({
    enableContentStream: contentStreamEnabled,
    enableRelease: !releasesSectionEnabled,
  });

  return (
    <SectionGrid $columns="2fr 1fr">
      <StyledNewsSectionComponent title="News"
                                  $enabled={contentStreamEnabled}
                                  headerActions={(
                                    <ToggleActionButton onClick={toggleNews}
                                                        isOpen={contentStreamEnabled} />
                                  )}>
        {contentStreamEnabled && (
          <>
            <ContentStreamNews />
            <ContentStreamNewsFooter />
          </>
        )}
      </StyledNewsSectionComponent>
      <StyledReleaseSectionComponent title="Releases"
                                     $enabled={releasesSectionEnabled}
                                     headerActions={(
                                       <ToggleActionButton onClick={toggleRelease}
                                                           isOpen={releasesSectionEnabled} />
                                     )}>
        {releasesSectionEnabled && (
          <ContentStreamReleasesSection />
        )}
      </StyledReleaseSectionComponent>
    </SectionGrid>
  );
};

export default ContentStreamSection;
