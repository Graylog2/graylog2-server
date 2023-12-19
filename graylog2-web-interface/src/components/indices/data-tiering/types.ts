export const DATA_TIERING_TYPE = {
  HOT_ONLY: 'hot_only',
  HOT_WARM: 'hot_warm',
} as const;

type DataTieringType = typeof DATA_TIERING_TYPE[keyof typeof DATA_TIERING_TYPE];

export type DataTieringConfig = {
  type: DataTieringType
  index_lifetime_min: string,
  index_lifetime_max: string,
  index_hot_lifetime_min?: string,
  warm_tier_enabled: boolean,
  archive_before_deletion: boolean,
}

export type DataTieringFormValues = {
  type: DataTieringType
  index_lifetime_min: number,
  index_lifetime_max: number,
  index_hot_lifetime_min?: number,
  warm_tier_enabled: boolean,
  archive_before_deletion: boolean,
}
