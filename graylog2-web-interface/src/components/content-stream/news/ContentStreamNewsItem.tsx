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
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';
import DOMPurify from 'dompurify';

import { Carousel, Timestamp } from 'components/common';
import { Panel } from 'components/bootstrap';
import type { FeedItem, FeedMediaContent } from 'components/content-stream/hook/useContentStream';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type Props = {
  feed: FeedItem
}

const StyledImage = styled.img(({ theme }: { theme: DefaultTheme }) => css`
  max-width: 100%;
  width: 100%;
  object-fit: contain;
  border-radius: ${theme.spacings.xxs} ${theme.spacings.xxs} 0 0;
`);
const StyledPanelBody = styled(Panel.Body)(({ theme }: { theme: DefaultTheme }) => css`
  flex-grow: 1;
  background-color: ${theme.colors.newsCards.background};

  > a {
    font-weight: bold;
  }
`);
const StyledPanelFooter = styled(Panel.Footer)(({ theme }: { theme: DefaultTheme }) => css`
  background-color: ${theme.colors.newsCards.background};
  border-radius: 0 0 ${theme.spacings.xxs} ${theme.spacings.xxs};
`);
const StyledPanel = styled(Panel)(({ theme }: { theme: DefaultTheme }) => css`
  display: flex;
  flex-direction: column;
  height: 100%;
  border: none;
  border-radius: ${theme.spacings.xxs};
`);
const _sanitizeText = (text) => DOMPurify.sanitize(text);

const getImage = (media: FeedMediaContent | Array<FeedMediaContent>) => (Array.isArray(media) ? media?.[0]?.attr_url : media?.attr_url);

const ContentStreamNewsItem = ({ feed }: Props) => {
  const sendTelemetry = useSendTelemetry();

  const handleSendTelemetry = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONTENTSTREAM.ARTICLE_CLICKED, {
      app_pathname: 'welcome',
      app_section: 'content-stream',
      event_details: {
        title: feed?.title,
        link: feed?.link,
      },
    });
  };

  return (
    <Carousel.Slide>
      <StyledPanel>
        <a href={feed?.link} onClick={() => handleSendTelemetry()} target="_blank" rel="noreferrer">
          <StyledImage src={getImage(feed?.['media:content'])} alt={feed?.title} />
        </a>

        <StyledPanelBody>
          <a href={feed?.link}
             target="_blank"
             onClick={() => handleSendTelemetry()}
             rel="noreferrer">
            {/* eslint-disable-next-line react/no-danger */}
            <span dangerouslySetInnerHTML={{ __html: _sanitizeText(feed?.title) }} />
          </a>
        </StyledPanelBody>
        <StyledPanelFooter>
          <Timestamp dateTime={_sanitizeText(feed?.pubDate)} format="shortReadable" />
        </StyledPanelFooter>
      </StyledPanel>
    </Carousel.Slide>
  );
};

export default ContentStreamNewsItem;
