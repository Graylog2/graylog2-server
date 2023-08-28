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

import Carousel from 'components/common/carousel/Carousel';
import useContentStream from 'components/content-stream/hook/useContentStream';
import { Spinner, ExternalLink } from 'components/common';
import ContentStreamNewsItem from 'components/content-stream/news/ContentStreamNewsItem';
import { Alert } from 'components/bootstrap';

import Icon from '../common/Icon';

const ContentStreamNews = () => {
  const { feedList, isLoadingFeed, error } = useContentStream();

  if (isLoadingFeed) {
    return <Spinner />;
  }

  if (error || isEmpty(feedList)) {
    return (
      <Alert bsStyle="info">
        <p>
          <Icon name="exclamation-triangle" /> Unable to load RSS feed at the moment ! You can read more
          on {' '}
          <ExternalLink href="https://www.graylog.org/blog/">
            Graylog
          </ExternalLink>
          .
        </p>
      </Alert>
    );
  }

  return (
    <Carousel>
      {feedList?.map((feed) => <ContentStreamNewsItem key={feed?.guid['#text'] || feed?.title} feed={feed} />)}
    </Carousel>
  );
};

export default ContentStreamNews;
