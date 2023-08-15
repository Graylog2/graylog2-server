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
import { Button } from 'components/bootstrap';
import SectionComponent from 'components/common/Section/SectionComponent';
import ContentStreamNews from 'components/content-stream/ContentStreamNews';
import ContentStreamNewsFooter from 'components/content-stream/news/ContentStreamNewsFooter';
import AppConfig from 'util/AppConfig';
import ContentStreamReleasesSection from 'components/content-stream/ContentStreamReleasesSection';
import useContentStreamSettings from 'components/content-stream/hook/useContentStreamSettings';
import { Icon } from 'components/common';
import useCurrentUser from 'hooks/useCurrentUser';

const StyledNewsSectionComponent = styled(SectionComponent)(({ theme }) => css`
  overflow: hidden;
  flex-grow: 3;
  height: min-content;
  @media (max-width: ${theme.breakpoints.max.md}) {
    flex-grow: 1;
  }
`);
const StyledReleaseSectionComponent = styled(SectionComponent)`
  flex-grow: 1;
  height: min-content;
`;
const StyledButton = styled(Button)(({ theme }) => css`
  border: 0;
  font-size: ${theme.fonts.size.large};

  &:hover {
    text-decoration: none;
  }
`);

const ContentStreamSection = () => {
  const { rss_url } = AppConfig.contentStream() || {};
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

  return (
    rss_url && (
      <SectionGrid $columns="2fr 1fr">
        <StyledNewsSectionComponent title="News"
                                    headerActions={(
                                      <StyledButton bsStyle="link"
                                                    onClick={() => updateContentStreamSettings({
                                                      enableContentStream: !contentStreamEnabled,
                                                      enableRelease: releasesSectionEnabled,
                                                    })}
                                                    type="button">Close
                                        <Icon name={contentStreamEnabled ? 'angle-down' : 'angle-right'} fixedWidth />
                                      </StyledButton>
                                    )}>
          {contentStreamEnabled && (
            <>
              <ContentStreamNews rssUrl={rss_url} />
              <ContentStreamNewsFooter />
            </>
          )}
        </StyledNewsSectionComponent>
        <StyledReleaseSectionComponent title="Releases"
                                       headerActions={(
                                         <StyledButton bsStyle="link"
                                                       onClick={() => updateContentStreamSettings({
                                                         enableContentStream: contentStreamEnabled,
                                                         enableRelease: !releasesSectionEnabled,
                                                       })}
                                                       type="button">Close
                                           <Icon name={releasesSectionEnabled ? 'angle-down' : 'angle-right'}
                                                 fixedWidth />
                                         </StyledButton>
                                       )}>
          {releasesSectionEnabled && (
            <ContentStreamReleasesSection rssUrl={rss_url} />
          )}
        </StyledReleaseSectionComponent>
      </SectionGrid>
    )
  );
};

export default ContentStreamSection;
