import { useQuery } from '@tanstack/react-query';
import { XMLParser } from 'fast-xml-parser';

import UserNotification from 'preflight/util/UserNotification';

export type FeedITem = {
  title: string,
  category: string,
  'content:encoded': string,
  description: string,
  pubDate: string,
  'dc:creator': string,
  link: string,
  'post-id': string,
  guid: string
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
const FEED_URL = 'http://localhost:8010/proxy';

const parseXML = (text: string) => {
  const parser = new XMLParser();
  const parsed = parser.parse(text);

  const { rss: { channel: { item: items = undefined } } } = parsed as RssFeed;

  return Promise.resolve(items);
};

export const fetchNewsFeed = () => window.fetch(FEED_URL, { method: 'GET' })
  .then((response) => response.text())
  .then(parseXML);
export const NEWS_FEED_QUERY_KEY = 'news_feed_query_key';

const useContentStream = () => {
  const { data, isLoading } = useQuery<Array<FeedITem>, Error>([NEWS_FEED_QUERY_KEY], () => fetchNewsFeed(), {
    onError: (errorThrown) => {
      UserNotification.error(`Loading news feed failed with status: ${errorThrown}`,
        'Could not load news feed');
    },
    initialData: [],
  });

  return {
    newsList: data,
    isLoadingFeed: isLoading,
  };
};

export default useContentStream;
