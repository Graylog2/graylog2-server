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
import { SystemContentPacks } from '@graylog/server-api';

import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';

export const createContentPack = (pack: unknown) => SystemContentPacks.createContentPack(pack as any);

export const deleteContentPack = (contentPackId: string) => SystemContentPacks.deleteContentPack(contentPackId);

export const deleteContentPackRev = (contentPackId: string, revision: number) =>
  SystemContentPacks.deleteContentPackBycontentPackIdAndrevision(contentPackId, revision);

export const installContentPack = (
  contentPackId: string,
  contentPackRev: number,
  parameters: unknown,
  shareRequest: EntitySharePayload,
) =>
  SystemContentPacks.installContentPack(contentPackId, contentPackRev, {
    entity: parameters,
    share_request: shareRequest,
  } as any);

export const uninstallContentPack = (contentPackId: string, installId: string) =>
  SystemContentPacks.deleteContentPackInstallationById(contentPackId, installId);

export const fetchUninstallDetails = (contentPackId: string, installId: string) =>
  SystemContentPacks.uninstallDetails(contentPackId, installId);
