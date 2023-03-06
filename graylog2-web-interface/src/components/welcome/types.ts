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

export type RecentActivityType = 'create' | 'update' | 'share' | 'delete'
export type Pagination = {
  page: number,
  per_page: number,
  count: number,
  total: number,
}

export type FavoriteItem = {
  grn: string,
  title: string
}

export type LastOpenedItem = {
  timestamp: string,
  grn: string,
  title: string
}

export type RecentActivityResponseItem = {
  item_grn: string,
  id: string,
  activity_type: RecentActivityType,
  item_title: string,
  timestamp: string,
  user_name?: string,
}

export type RecentActivityItem = {
  id: string,
  activityType: RecentActivityType,
  itemTitle,
  itemGrn: string,
  timestamp: string,
  userName?: string
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

export type PaginatedFavoriteItems = {
  favorites: Array<FavoriteItem>,
  page: number,
  per_page: number,
  count: number,
  total: number,
}

export type RequestQuery = {
  page: number,
};
