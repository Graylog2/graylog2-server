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
import PropTypes from 'prop-types';

export const DATA_TIERING_TYPE = {
  HOT_ONLY: 'hot_only',
  HOT_WARM: 'hot_warm',
} as const;

export type DataTieringType = typeof DATA_TIERING_TYPE[keyof typeof DATA_TIERING_TYPE];

export type DataTieringConfig = {
  type: DataTieringType
  index_lifetime_min: string,
  index_lifetime_max: string,
  index_hot_lifetime_min?: string,
  warm_tier_enabled?: boolean,
  archive_before_deletion?: boolean,
  warm_tier_repository_name?: string | null,
}

export type DataTieringFormValues = {
  type: DataTieringType
  index_lifetime_min: number,
  index_lifetime_max: number,
  index_hot_lifetime_min?: number,
  warm_tier_enabled: boolean,
  archive_before_deletion: boolean,
  warm_tier_repository_name?: string | null,
}

export type DataTieringStatus = {
  has_failed_snapshot: boolean,
  failed_snapshot_name: string | null,
}

export const dataTieringPropType = PropTypes.shape({
  type: PropTypes.oneOf(['hot_only', 'hot_warm']).isRequired,
  index_lifetime_min: PropTypes.string.isRequired,
  index_lifetime_max: PropTypes.string.isRequired,
  index_hot_lifetime_min: PropTypes.string,
  warm_tier_enabled: PropTypes.bool,
  archive_before_deletion: PropTypes.bool,
  warm_tier_repository_name: PropTypes.string,
});
