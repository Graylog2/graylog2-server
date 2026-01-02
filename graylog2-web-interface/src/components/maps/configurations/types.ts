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

export type GeoVendorType = 'MAXMIND' | 'IPINFO';
export type TimeUnit = 'SECONDS' | 'MINUTES' | 'HOURS' | 'DAYS';
export const CLOUD_STORAGE_OPTION = {
  GCS: 'gcs',
  S3: 's3',
  ABS: 'abs'
} as const;

export type EncryptedValue =
  | { is_set: boolean }
  | { set_value: string }
  | { keep_value: boolean }
  | { delete_value: boolean };

export type GeoIpConfigType = {
  enabled: boolean;
  enforce_graylog_schema: boolean;
  db_vendor_type: GeoVendorType;
  city_db_path: string;
  asn_db_path: string;
  refresh_interval_unit: TimeUnit;
  refresh_interval: number;
  pull_from_cloud?: (typeof CLOUD_STORAGE_OPTION)[keyof typeof CLOUD_STORAGE_OPTION];
  gcs_project_id?: string;
  azure_endpoint?: string;
  azure_account_key?: EncryptedValue;
  azure_account?: string;
  azure_container?: string;
};

export type OptionType = {
  value: string;
  label: string;
};
