export type EntityItemType = 'search' | 'dashboard';
export type RecentActivityType = 'create' | 'update' | 'share' | 'delete'
export type Pagination = {
    page: number,
    per_page: number,
    count: number,
    total: number,
}

export type PinnedItem = {
  id: string,
  type: EntityItemType,
  title: string
}

export type LastOpenedItem = {
  id: string,
  type: EntityItemType,
  title: string
}

export type RecentActivityResponseItem = {
  id: string,
  activity_type: RecentActivityType,
  item_type: EntityItemType,
  item_id: string,
  title: string,
  timestamp: string
}

export type RecentActivityItem = {
  id: string,
  activityType: RecentActivityType,
  itemType: EntityItemType,
  itemId: string,
  title: string,
  timestamp: string
}

export type PaginatedResponseRecentActivity = {
  recentActivity: Array<RecentActivityResponseItem>,
  page: number,
  per_page: number,
  count: number,
  total: number,
}

export type PaginatedRecentActivity = {
  recentActivity: Array<RecentActivityItem>,
  page: number,
  per_page: number,
  count: number,
  total: number,
}

export type PaginatedLastOpened = {
  lastOpened: Array<LastOpenedItem>,
  page: number,
  per_page: number,
  count: number,
  total: number,
}

export type PaginatedPinnedItems = {
  pinnedItems: Array<PinnedItem>,
  page: number,
  per_page: number,
  count: number,
  total: number,
}

export type RequestQuery = {
  page: number,
};
