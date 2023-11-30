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
import uniq from 'lodash/uniq';

import RowCheckbox from 'components/common/EntityDataTable/RowCheckbox';
import { BULK_SELECT_COLUMN_WIDTH } from 'components/common/EntityDataTable/Constants';
import type { EntityBase } from 'components/common/EntityDataTable/types';

type CheckboxStatus = 'CHECKED' | 'UNCHECKED' | 'PARTIAL';

const useCheckboxStatus = <Entity extends EntityBase>(data: Readonly<Array<Entity>>, selectedEntityIds: Array<string>) => {
  const checkboxRef = useRef<HTMLInputElement>();
  const checkboxStatus: CheckboxStatus = useMemo(() => {
    const selectedEntities = data.filter(({ id }) => selectedEntityIds.includes(id));

    if (selectedEntities.length === 0) {
      return 'UNCHECKED';
    }

    if (selectedEntities.length === data.length) {
      return 'CHECKED';
    }

    return 'PARTIAL';
  }, [data, selectedEntityIds]);

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

type Props<Entity extends EntityBase> = {
  data: Readonly<Array<Entity>>,
  selectedEntities: Array<string>,
  setSelectedEntities: React.Dispatch<React.SetStateAction<Array<string>>>,
}

const BulkSelectHead = <Entity extends EntityBase>({
  data,
  setSelectedEntities,
  selectedEntities,
}: Props<Entity>) => {
  const { checkboxRef, checkboxStatus } = useCheckboxStatus(data, selectedEntities);
  const title = `${checkboxStatus === 'CHECKED' ? 'Deselect' : 'Select'} all visible entities`;

  const onBulkSelect = () => {
    setSelectedEntities((cur) => {
      const entityIds = data.map(({ id }) => id);

      if (checkboxStatus === 'CHECKED') {
        return cur.filter((itemId) => !entityIds.includes(itemId));
      }

      return uniq([...cur, ...entityIds]);
    });
  };

  return (
    <td style={{ width: `${BULK_SELECT_COLUMN_WIDTH}px` }}>
      <RowCheckbox inputRef={(ref) => { checkboxRef.current = ref; }}
                   onChange={onBulkSelect}
                   checked={checkboxStatus === 'CHECKED'}
                   title={title}
                   disabled={!data?.length}
                   aria-label={title} />
    </td>
  );
};

export default BulkSelectHead;
