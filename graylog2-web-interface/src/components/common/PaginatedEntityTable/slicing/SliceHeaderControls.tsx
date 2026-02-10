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
import styled, { css } from 'styled-components';

import { DropdownButton, Button } from 'components/bootstrap';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import MenuItem from 'components/bootstrap/menuitem/MenuItem';
import { defaultCompare } from 'logic/DefaultCompare';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

const SliceHeader = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.xs};
    justify-content: space-between;
    margin-bottom: ${theme.spacings.sm};
  `,
);

type Props = {
  appSection: string;
  activeColumnTitle: string | undefined;
  activeSlice: string | undefined;
  sliceCol: string | undefined;
  columnSchemas: Array<ColumnSchema>;
  onChangeSlicing: (sliceCol: string | undefined, slice?: string | undefined) => void;
};

const SliceHeaderControls = ({
  appSection,
  activeColumnTitle,
  activeSlice,
  sliceCol,
  columnSchemas,
  onChangeSlicing,
}: Props) => {
  const sliceableColumns = columnSchemas
    .filter((schema) => schema.sliceable)
    .sort(({ title: title1 }, { title: title2 }) => defaultCompare(title1, title2));
  const sendTelemetry = useSendTelemetry();
  const onSliceColumn = (columnId: string) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.SLICE_COLUMN_SELECTED_SECTION, {
      app_section: appSection,
      app_action_value: 'slice-column-select',
      event_details: { attribute_id: columnId },
    });
    onChangeSlicing(columnId);
  };
  const onRemoveSlicing = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.SLICE_REMOVED, {
      app_section: appSection,
      app_action_value: 'slice-remove',
      event_details: { attribute_id: sliceCol },
    });
    onChangeSlicing(undefined, undefined);
  };

  return (
    <SliceHeader>
      <DropdownButton bsSize="small" title={activeColumnTitle ?? 'Slice by'}>
        <MenuItem header>Slice by</MenuItem>
        {sliceableColumns.map((schema) => (
          <MenuItem key={schema.id} onClick={() => onSliceColumn(schema.id)}>
            {schema.title}
          </MenuItem>
        ))}
        <MenuItem divider />
        <MenuItem onClick={onRemoveSlicing}>No slicing</MenuItem>
      </DropdownButton>
      {activeSlice && (
        <Button bsStyle="link" bsSize="sm" onClick={() => onChangeSlicing(sliceCol, undefined)}>
          Clear slice
        </Button>
      )}
    </SliceHeader>
  );
};

export default SliceHeaderControls;
