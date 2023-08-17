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
import { useQuery } from '@tanstack/react-query';
import { XMLParser } from 'fast-xml-parser';

import UserNotification from 'preflight/util/UserNotification';

export type FeedITem = {
  title?: string,
  link?: string,
  comments?: string,
  'dc:creator'?: string,
  pubDate?: string,
  category?: Array<string>
  guid?: {
    '#text'?: string,
    attr_isPermaLink?: string,
  },
  description?: string,
  'content:encoded'?: string,
  'wfw:commentRss'?: string,
  'slash:comments'?: number,
  'media:content'?: {
    'media:title'?: {
      '#text'?: string,
      attr_type?: string,
    },
    'media:thumbnail'?: {
      attr_url?: string,
      attr_width?: string,
      attr_height?: string,
    },
    'media:copyright'?: string,
    attr_url?: string,
    attr_type?: string,
    attr_medium?: string,
    attr_width?: string,
    attr_height?: string,
  }
}

type RssFeed = {
  rss: {
    channel: {
      'atom:link': string,
      description: string
      generator: string,
      image: string
      item: Array<FeedITem>,
      language: string
      lastBuildDate: string,
      link: string
      site: number
      'sy:updateFrequency': number
      'sy:updatePeriod': string
      title: string
    }
  }
}

const parseXML = (text: string): Array<FeedITem> => {
  const options = {
    ignoreAttributes: false,
    attributeNamePrefix: 'attr_',
  };
  const parser = new XMLParser(options);

  const parsed = parser.parse(text);

  const { rss: { channel: { item: items = undefined } } } = parsed as RssFeed;

  return items;
};

export const fetchNewsFeed = (rssUrl: string) => window.fetch(rssUrl, { method: 'GET' })
  .then((response) => response.text())
  .then(parseXML);
export const CONTENT_STREAM_CONTENT_KEY = ['content-stream', 'content'];

const useContentStream = (rssUrl: string): { isLoadingFeed: boolean, feedList: Array<FeedITem> } => {
  const {
    data,
    isLoading,
  } = useQuery<Array<FeedITem>, Error>([...CONTENT_STREAM_CONTENT_KEY, rssUrl], () => fetchNewsFeed(rssUrl), {
    onError: (errorThrown) => {
      UserNotification.error(`Loading news feed failed with status: ${errorThrown}`,
        'Could not load news feed');
    },
    initialData: [],
  });

  return {
    feedList: data,
    isLoadingFeed: isLoading,
  };
};

export default useContentStream;
