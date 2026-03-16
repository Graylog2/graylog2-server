import * as React from 'react';

import RowCheckbox from 'components/common/EntityDataTable/RowCheckbox';
import type { SelectableMessageTableMessage } from 'views/components/widgets/MessageList';
import useSelectedMessageEntities from 'views/hooks/useSelectedMessageEntities';

const BulkSelectHead = ({ data }: { data: Array<SelectableMessageTableMessage> }) => {
  const { toggleAllEntitySelect, isAllRowsSelected, isSomeRowsSelected } = useSelectedMessageEntities();
  const title = `${isAllRowsSelected ? 'Deselect' : 'Select'} all visible messages`;

  const onBulkSelect = () => toggleAllEntitySelect(data);

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
