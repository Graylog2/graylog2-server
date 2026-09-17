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
import isEmpty from 'lodash/isEmpty';

import { ListGroup, Alert } from 'components/bootstrap';
import { RelativeTime, Sanitize, Spinner, ExternalLink } from 'components/common';
import { StyledListGroupItem, TimeInfo } from 'components/welcome/EntityListItem';
import type { FeedItem } from 'components/content-stream/hook/useContentStream';
import useContentStream from 'components/content-stream/hook/useContentStream';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const ContentStreamReleasesSection = () => {
  const path = 'release-info';
  const { feedList, isLoadingFeed, error } = useContentStream(path);
  const sendTelemetry = useSendTelemetry('content-stream');

  const handleSendTelemetry = (feed: FeedItem) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONTENTSTREAM.RELESE_ARTICLE_CLICKED, {
      app_pathname: 'welcome',
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
          Unable to load RSS feed at the moment ! You can read more on{' '}
          <ExternalLink href="https://www.graylog.org/post/tag/release-info">Graylog.org</ExternalLink>.
        </p>
      </Alert>
    );
  }

  return (
    <ListGroup>
      {feedList.map((feed) => (
        <StyledListGroupItem key={feed?.guid['#text'] || feed?.title}>
          <a href={feed?.link} onClick={() => handleSendTelemetry(feed)} target="_blank" rel="noreferrer">
            <Sanitize html={feed?.title} />
          </a>
          {feed?.pubDate ? (
            <TimeInfo>
              <RelativeTime dateTime={feed.pubDate} />
            </TimeInfo>
          ) : null}
        </StyledListGroupItem>
      ))}
    </ListGroup>
  );
};

export default ContentStreamReleasesSection;
