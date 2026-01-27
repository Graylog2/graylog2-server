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
import styled from 'styled-components';

import { DropdownButton } from 'components/bootstrap';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import MenuItem from 'components/bootstrap/menuitem/MenuItem';
import { defaultCompare } from 'logic/DefaultCompare';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

const Container = styled.div`
  min-width: 300px;
`;

type Props = {
  appSection: string;
  sliceCol: string;
  columnSchemas: Array<ColumnSchema>;
  onChangeSlicing: (sliceCol: string | undefined, slice?: string) => void;
};

const Slicing = ({ appSection, sliceCol, columnSchemas, onChangeSlicing }: Props) => {
  const sendTelemetry = useSendTelemetry();
  const sliceableColumns = columnSchemas
    .filter((schema) => schema.sliceable)
    .sort(({ title: title1 }, { title: title2 }) => defaultCompare(title1, title2));
  const activeColumn = sliceableColumns.find(({ id }) => id === sliceCol);
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
    <Container>
      <DropdownButton bsSize="small" id="slicing-dropdown" title={activeColumn?.title ?? 'Slice by'}>
        <MenuItem header>Slice by</MenuItem>
        {sliceableColumns.map((schema) => (
          <MenuItem key={schema.id} onClick={() => onSliceColumn(schema.id)}>
            {schema.title}
          </MenuItem>
        ))}
        <MenuItem divider />
        <MenuItem onClick={onRemoveSlicing}>No slicing</MenuItem>
      </DropdownButton>
    </Container>
  );
};

export default Slicing;
