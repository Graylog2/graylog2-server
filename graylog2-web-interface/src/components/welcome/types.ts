export type EntityItemType = 'search' | 'dashboard';
export type ActivityType = 'create' | 'update' | 'share' | 'delete'
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

export type LastOpenItem = {
  id: string,
  type: EntityItemType,
  title: string
}

export type RecentActivityResponseItem = {
  id: string,
  activity_type: ActivityType,
  item_type: EntityItemType,
  item_id: string,
  title: string,
  timestamp: string
}

export type RecentActivityItem = {
  id: string,
  activityType: ActivityType,
  itemType: EntityItemType,
  itemId: string,
  title: string,
  timestamp: string
}

export type PaginatedResponseRecentActivities = {
  activities: Array<RecentActivityResponseItem>,
  pagination: Pagination,
}

export type PaginatedRecentActivities = {
  activities: Array<RecentActivityItem>,
  pagination: Pagination,
}

export type PaginatedLastOpen = {
  activities: Array<LastOpenItem>,
  pagination: Pagination,
}

export type PaginatedPinned = {
  activities: Array<PinnedItem>,
  pagination: Pagination,
}

export type RequestQuery = {
  page: number,
};
