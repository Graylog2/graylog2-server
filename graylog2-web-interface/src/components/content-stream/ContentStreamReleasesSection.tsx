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
import isEmpty from 'lodash/isEmpty';
import DOMPurify from 'dompurify';

import { ListGroup, ListGroupItem, Label, Alert } from 'components/bootstrap';
import { RelativeTime, Spinner, ExternalLink } from 'components/common';
import type { FeedItem } from 'components/content-stream/hook/useContentStream';
import useContentStream from 'components/content-stream/hook/useContentStream';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const StyledListGroupItem = styled(ListGroupItem)(({ theme }: { theme: DefaultTheme }) => css`
  display: flex;
  gap: ${theme.spacings.md};
  align-items: flex-start;
`);
const LastOpenedTime = styled.i(({ theme }: { theme: DefaultTheme }) => css`
  color: ${theme.colors.gray[60]};
`);
export const StyledLabel = styled(Label)`
  cursor: default;
  width: 110px;
  display: block;
`;

const _sanitizeText = (text = '') => DOMPurify.sanitize(text);

const ContentStreamReleasesSection = () => {
  const path = 'release-info';
  const { feedList, isLoadingFeed, error } = useContentStream(path);
  const sendTelemetry = useSendTelemetry();

  const handleSendTelemetry = (feed: FeedItem) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONTENTSTREAM.RELESE_ARTICLE_CLICKED, {
      app_pathname: 'welcome',
      app_section: 'content-stream',
      event_details: {
        title: feed?.title,
        link: feed?.link,
      },
    });
  };

  if (isLoadingFeed && !isEmpty(feedList)) {
    return <Spinner />;
  }

  if (error || isEmpty(feedList)) {
    return (
      <Alert bsStyle="info">
        <p>
          Unable to load RSS feed at the moment ! You can read more
          on {' '}
          <ExternalLink href="https://www.graylog.org/post/tag/release-info">
            Graylog.org
          </ExternalLink>
          .
        </p>
      </Alert>
    );
  }

  return (
    <ListGroup>
      {feedList.map((feed) => (
        <StyledListGroupItem key={feed?.guid['#text'] || feed?.title}>
          <a href={feed?.link} onClick={() => handleSendTelemetry(feed)} target="_blank" rel="noreferrer">
            {/* eslint-disable-next-line react/no-danger */}
            <span dangerouslySetInnerHTML={{ __html: _sanitizeText(feed?.title) }} />
          </a>
          {feed?.pubDate ? <LastOpenedTime><RelativeTime dateTime={feed.pubDate} /></LastOpenedTime> : null}
        </StyledListGroupItem>
      ))}
    </ListGroup>
  );
};

export default ContentStreamReleasesSection;
