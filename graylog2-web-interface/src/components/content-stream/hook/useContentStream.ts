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
import { useCallback } from 'react';

import { DEFAULT_FEED } from 'components/content-stream/Constants';
import AppConfig from 'util/AppConfig';
import useContentStreamSettings from 'components/content-stream/hook/useContentStreamSettings';

export type FeedMediaContent = {
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
};
export type FeedItem = {
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
  'media:content'?: Array<FeedMediaContent> | FeedMediaContent
}

type RssFeed = {
  rss: {
    channel: {
      'atom:link': string,
      description: string
      generator: string,
      image: string
      item: Array<FeedItem> | FeedItem,
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

const parseXML = (text: string): Array<FeedItem> => {
  const options = {
    ignoreAttributes: false,
    attributeNamePrefix: 'attr_',
  };
  const parser = new XMLParser(options);

  const parsed = parser.parse(text);

  const { rss: { channel: { item: items = undefined } } } = parsed as RssFeed;

  return Array.isArray(items) ? items : [items];
};

export const fetchNewsFeed = (rssUrl: string) => rssUrl && window.fetch(rssUrl, { method: 'GET' })
  .then((response) => response.text())
  .then(parseXML)
  .catch((error) => error);
export const CONTENT_STREAM_CONTENT_KEY = ['content-stream', 'content'];

const useContentStream = (path?: string): { isLoadingFeed: boolean, feedList: Array<FeedItem>, error: Error } => {
  const { contenStreamTags: { currentTag, isLoadingTags, contentStreamTagError } } = useContentStreamSettings();
  const { rss_url } = AppConfig.contentStream() || {};

  const getDefaultTag = useCallback(() => {
    if (path) {
      return path;
    }

    if (isLoadingTags || contentStreamTagError || !currentTag) {
      return DEFAULT_FEED;
    }

    return currentTag;
  }, [contentStreamTagError, currentTag, isLoadingTags, path]);

  const rssUrl = rss_url && `${rss_url}/${getDefaultTag()}/feed`;

  const {
    data,
    isLoading,
    error,
  } = useQuery<Array<FeedItem>, Error>([...CONTENT_STREAM_CONTENT_KEY, rssUrl], () => fetchNewsFeed(rssUrl), {
    initialData: [],
  });

  return {
    error,
    feedList: data,
    isLoadingFeed: isLoading,
  };
};

export default useContentStream;
