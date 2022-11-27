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
import { useMemo, useEffect, useRef } from 'react';
import { uniq } from 'lodash';

import RowCheckbox from 'components/common/EntityDataTable/RowCheckbox';

type CheckboxStatus = 'CHECKED' | 'UNCHECKED' | 'PARTIAL';

const useCheckboxStatus = (rows, selectedEntities) => {
  const checkboxRef = useRef<HTMLInputElement>();

  const checkboxStatus: CheckboxStatus = useMemo(() => {
    const selectedRows = rows.filter(({ id }) => selectedEntities.includes(id));

    if (selectedRows.length === 0) {
      return 'UNCHECKED';
    }

    if (selectedRows.length === rows.length) {
      return 'CHECKED';
    }

    return 'PARTIAL';
  }, [rows, selectedEntities]);

  useEffect(() => {
    if (checkboxRef.current) {
      if (checkboxStatus === 'PARTIAL') {
        checkboxRef.current.indeterminate = true;

        return;
      }

      checkboxRef.current.indeterminate = false;
    }
  }, [checkboxStatus]);

  return {
    checkboxRef,
    checkboxStatus,
  };
};

type Props<Entity extends { id: string }> = {
  data: Array<Entity>
  selectedEntities: Array<string>,
  setSelectedEntities: React.Dispatch<React.SetStateAction<Array<string>>>
}

const BulkActionsHead = <Entity extends { id: string }>({
  data,
  setSelectedEntities,
  selectedEntities,
}: Props<Entity>) => {
  const { checkboxRef, checkboxStatus } = useCheckboxStatus(data, selectedEntities);
  const title = `${checkboxStatus === 'CHECKED' ? 'Deselect' : 'all visible entities'}`;

  const onBulkSelect = () => {
    setSelectedEntities((cur) => {
      const rowsIds = data.map(({ id }) => id);

      if (checkboxStatus === 'CHECKED') {
        return cur.filter((itemId) => !rowsIds.includes(itemId));
      }

      return uniq([...cur, ...rowsIds]);
    });
  };

  return (
    <td>
      <RowCheckbox inputRef={(ref) => { checkboxRef.current = ref; }}
                   onChange={onBulkSelect}
                   checked={checkboxStatus === 'CHECKED'}
                   title={title}
                   disabled={!data?.length} />
    </td>
  );
};

export default BulkActionsHead;
