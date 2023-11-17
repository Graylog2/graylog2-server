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

export type ContentPackInstallation = {
  created_at: string,
  description: string,
  entities?: Array<ContentPackEntity>,
  id: string,
  name: string,
  parameters?: Array<any>,
  rev: number,
  server_version: string,
  summary: string,
  url: string,
  v: number,
  vendor: string,
}

export type ContentPackVersionsType = {
  contentPacks: Array<ContentPackInstallation>,
  latest: ContentPackInstallation,
  latestRevision: number,
  revisions: Array<number>
}

export type ContentPackEntity = {
  id: string,
  type: EntityType,
  v: string,
  data: Data,
  constraints: Array<Constraint>,
}

export interface EntityType {
  name: string;

  version: string;
}

export interface Data {
  [key: string]: Type | { [key: string]: Type },
}

export interface Type {
  '@type': string,

  '@value': string,
}

export interface Constraint {
  type: string,

  plugin?: string,

  version: string,
}

export type ContentPackMetadata = {
  [key: number]: {
    [key: number]: {
      [key: string]: number,
    },
  },
}
