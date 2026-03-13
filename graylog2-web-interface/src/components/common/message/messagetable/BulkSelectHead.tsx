import * as React from 'react';
import uniq from 'lodash/uniq';

import RowCheckbox from 'components/common/EntityDataTable/RowCheckbox';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import type { EntityBase } from 'components/common/EntityDataTable/types';

type Props<Entity extends EntityBase> = {
  data: Readonly<Array<Entity>>;
};

const BulkSelectHead = <Entity extends EntityBase>({ data }: Props<Entity>) => {
  const { setSelectedEntities, isAllRowsSelected, isSomeRowsSelected } = useSelectedEntities();
  const title = `${isAllRowsSelected ? 'Deselect' : 'Select'} all visible messages`;

  const onBulkSelect = () => {
    setSelectedEntities((cur) => {
      const entityIds = data.map(({ id }) => id);

      if (isAllRowsSelected) {
        return cur.filter((itemId) => !entityIds.includes(itemId));
      }

      return uniq([...cur, ...entityIds]);
    });
  };

  return (
    <RowCheckbox
      indeterminate={isSomeRowsSelected}
      onChange={onBulkSelect}
      checked={isAllRowsSelected}
      title={title}
      disabled={!data.length}
      aria-label={title}
    />
  );
};

export default BulkSelectHead;
