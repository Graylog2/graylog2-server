import type { PAGE_TYPE, ENTITY_TYPE } from 'components/quick-jump/Constants';

export type SearchResultItem = {
  type: typeof PAGE_TYPE | typeof ENTITY_TYPE;
  link: string;
  title: string;
  backendScore?: number;
};
