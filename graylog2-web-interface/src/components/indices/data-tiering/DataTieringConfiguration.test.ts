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
import { prepareDataTieringConfig } from 'components/indices/data-tiering/DataTieringConfiguration';
import { DATA_TIERING_TYPE } from 'components/indices/data-tiering';
import type { DataTieringFormValues } from 'components/indices/data-tiering';

const hotWarmPluginStore = { exports: () => [{ type: DATA_TIERING_TYPE.HOT_WARM }] };
const hotOnlyPluginStore = { exports: () => [] as Array<{ type: string }> };

const formValues = (values: Partial<DataTieringFormValues>) =>
  ({
    type: DATA_TIERING_TYPE.HOT_WARM,
    index_lifetime_min: 30,
    index_lifetime_max: 40,
    ...values,
  }) as DataTieringFormValues;

describe('prepareDataTieringConfig', () => {
  it('keeps the archive retention of an index set that archives', () => {
    const config = prepareDataTieringConfig(
      formValues({
        archive_before_deletion: true,
        archive_retention_override_enabled: true,
        archive_retention_time: 30,
      }),
      hotWarmPluginStore,
      false,
    );

    expect(config.archive_retention_override_enabled).toBe(true);
    expect(config.archive_retention_time).toBe(30);
  });

  it('drops the archive retention of an index set that does not archive', () => {
    const config = prepareDataTieringConfig(
      formValues({
        archive_before_deletion: false,
        archive_retention_override_enabled: true,
        archive_retention_time: 30,
      }),
      hotWarmPluginStore,
      false,
    );

    expect(config.archive_retention_override_enabled).toBe(false);
    expect(config.archive_retention_time).toBeNull();
  });

  it('stores no archive retention at all without the hot warm tier', () => {
    const config = prepareDataTieringConfig(
      formValues({
        archive_before_deletion: false,
        archive_retention_override_enabled: true,
        archive_retention_time: 30,
      }),
      hotOnlyPluginStore,
      false,
    );

    expect(config).not.toHaveProperty('archive_retention_override_enabled');
    expect(config).not.toHaveProperty('archive_retention_time');
  });
});
