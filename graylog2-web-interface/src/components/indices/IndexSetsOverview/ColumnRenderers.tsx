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
import * as React from 'react';

import type { ColumnRenderers } from 'components/common/EntityDataTable';
import { ExpandedSectionToggleWrapper } from 'components/common/EntityDataTable';
import type { ColumnRenderersByAttribute } from 'components/common/EntityDataTable/types';
import { Timestamp } from 'components/common';
import TextOverflowEllipsis from 'components/common/TextOverflowEllipsis';
import NumberUtils from 'util/NumberUtils';
import { formatReadableNumber } from 'util/NumberFormatting';

import { DETAILS_SECTION, INDEX_SET_CATEGORY_TITLES } from './Constants';
import type { IndexSetCategory, IndexSetEntity } from './types';
import TitleCell from './cells/TitleCell';
import FieldTypeProfileCell from './cells/FieldTypeProfileCell';
import MetricCell from './cells/MetricCell';
import DeflectorHealthCell from './cells/DeflectorHealthCell';
import { METRIC_COLUMN_IDS } from './metricColumns';

type ToggleProps = React.PropsWithChildren<{
  indexSet: IndexSetEntity;
  align?: 'right';
}>;

const DetailsToggle = ({ indexSet, align = undefined, children = undefined }: ToggleProps) => (
  <ExpandedSectionToggleWrapper id={indexSet.id} section={DETAILS_SECTION} align={align}>
    {children}
  </ExpandedSectionToggleWrapper>
);

const customColumnRenderers = (
  extensionColumnRenderers: ColumnRenderersByAttribute<IndexSetEntity>,
): ColumnRenderers<IndexSetEntity> => ({
  attributes: {
    title: {
      renderCell: (_title: string, indexSet: IndexSetEntity) => (
        <DetailsToggle indexSet={indexSet}>
          <TitleCell indexSet={indexSet} />
        </DetailsToggle>
      ),
      width: 0.5,
    },
    description: {
      renderCell: (description: string, indexSet: IndexSetEntity) => (
        <DetailsToggle indexSet={indexSet}>
          <TextOverflowEllipsis>{description}</TextOverflowEllipsis>
        </DetailsToggle>
      ),
    },
    index_prefix: {
      renderCell: (indexPrefix: string, indexSet: IndexSetEntity) => (
        <DetailsToggle indexSet={indexSet}>{indexPrefix}</DetailsToggle>
      ),
      width: 0.3,
    },
    index_template_type: {
      renderCell: (templateType: string, indexSet: IndexSetEntity) => (
        <DetailsToggle indexSet={indexSet}>{templateType}</DetailsToggle>
      ),
      width: 0.3,
    },
    category: {
      renderCell: (category: IndexSetCategory, indexSet: IndexSetEntity) => (
        <DetailsToggle indexSet={indexSet}>{INDEX_SET_CATEGORY_TITLES[category] ?? category}</DetailsToggle>
      ),
      staticWidth: 'matchHeader',
    },
    stream_count: {
      renderCell: (streamCount: number, indexSet: IndexSetEntity) => (
        <DetailsToggle indexSet={indexSet} align="right">
          {streamCount}
        </DetailsToggle>
      ),
      staticWidth: 'matchHeader',
      textAlign: 'right',
    },
    shards: {
      renderCell: (shards: number, indexSet: IndexSetEntity) => (
        <DetailsToggle indexSet={indexSet} align="right">
          {shards}
        </DetailsToggle>
      ),
      staticWidth: 'matchHeader',
      textAlign: 'right',
    },
    replicas: {
      renderCell: (replicas: number, indexSet: IndexSetEntity) => (
        <DetailsToggle indexSet={indexSet} align="right">
          {replicas}
        </DetailsToggle>
      ),
      staticWidth: 'matchHeader',
      textAlign: 'right',
    },
    field_type_refresh_interval: {
      renderCell: (_interval: unknown, indexSet: IndexSetEntity) => (
        <DetailsToggle indexSet={indexSet} align="right">
          {indexSet.field_type_refresh_interval / 1000.0} seconds
        </DetailsToggle>
      ),
      staticWidth: 'matchHeader',
      textAlign: 'right',
    },
    field_type_profile: {
      renderCell: (_profile: unknown, indexSet: IndexSetEntity) => (
        <DetailsToggle indexSet={indexSet}>
          <FieldTypeProfileCell profileId={indexSet.field_type_profile} />
        </DetailsToggle>
      ),
      width: 0.3,
    },
    creation_date: {
      renderCell: (creationDate: string, indexSet: IndexSetEntity) => (
        <DetailsToggle indexSet={indexSet}>
          <Timestamp dateTime={creationDate} />
        </DetailsToggle>
      ),
      staticWidth: 160,
    },
    [METRIC_COLUMN_IDS.indexCount]: {
      renderCell: (_value: unknown, indexSet: IndexSetEntity) => (
        <MetricCell indexSet={indexSet} field="index_count" renderValue={(count) => NumberUtils.formatNumber(count)} />
      ),
      staticWidth: 'matchHeader',
      textAlign: 'right',
    },
    [METRIC_COLUMN_IDS.documentCount]: {
      renderCell: (_value: unknown, indexSet: IndexSetEntity) => (
        <MetricCell
          indexSet={indexSet}
          field="document_count"
          renderValue={(count) => formatReadableNumber(count)}
        />
      ),
      staticWidth: 120,
      textAlign: 'right',
    },
    [METRIC_COLUMN_IDS.sizeBytes]: {
      renderCell: (_value: unknown, indexSet: IndexSetEntity) => (
        <MetricCell indexSet={indexSet} field="size_bytes" renderValue={(size) => NumberUtils.formatBytes(size)} />
      ),
      staticWidth: 120,
      textAlign: 'right',
    },
    [METRIC_COLUMN_IDS.deflectorHealth]: {
      renderCell: (_value: unknown, indexSet: IndexSetEntity) => (
        <MetricCell
          indexSet={indexSet}
          field="deflector_health"
          renderValue={(health) => <DeflectorHealthCell health={health} />}
        />
      ),
      staticWidth: 'matchHeader',
    },
    [METRIC_COLUMN_IDS.fieldCount]: {
      renderCell: (_value: unknown, indexSet: IndexSetEntity) => (
        <MetricCell indexSet={indexSet} field="field_count" renderValue={(count) => NumberUtils.formatNumber(count)} />
      ),
      staticWidth: 'matchHeader',
      textAlign: 'right',
    },
    ...extensionColumnRenderers,
  },
});

export default customColumnRenderers;
