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
import type SharedEntity from 'logic/permissions/SharedEntity';
import type { GRN } from 'logic/permissions/types';
import type { PaginatedList } from 'stores/PaginationTypes';
import type { SelectedGranteeCapabilities } from 'logic/permissions/EntityShareState';

export type PaginatedEntityShares = PaginatedList<SharedEntity> & {
  context: {
    granteeCapabilities: { [grn: string]: string };
  };
};

export type EntitySharePayload = {
  selected_grantee_capabilities?: SelectedGranteeCapabilities;
  prepare_request?: Array<GRN>;
  selected_collections?: Array<GRN>;
};

export type EntityShare = {
  share_request?: EntitySharePayload;
};
