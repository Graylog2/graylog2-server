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

import RowCheckbox from 'components/common/EntityDataTable/RowCheckbox';
import useSelectedMessageEntities from 'views/hooks/useSelectedMessageEntities';
import useSelectableMessageTableMessages from 'views/components/widgets/useSelectableMessageTableMessages';

const BulkSelectHead = () => {
  const { selectableMessageTableMessages: data } = useSelectableMessageTableMessages();
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
