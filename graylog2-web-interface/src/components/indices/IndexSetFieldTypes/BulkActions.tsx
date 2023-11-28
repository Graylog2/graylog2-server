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
import { useState } from 'react';

import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import MenuItem from 'components/bootstrap/MenuItem';
import IndexSetCustomFieldTypeRemoveModal
  from 'components/indices/IndexSetFieldTypes/IndexSetCustomFieldTypeRemoveModal';

type Props = {
  selectedFields: Array<string>,
  setSelectedFields: React.Dispatch<React.SetStateAction<Array<string>>>,
  indexSetId: string,
}

const BulkActions = ({ selectedFields, setSelectedFields, indexSetId }: Props) => {
  const [showResetModal, setShowResetModal] = useState<boolean>(false);
  const toggleResetModal = () => setShowResetModal((cur) => !cur);

  return (
    <BulkActionsDropdown selectedEntities={selectedFields} setSelectedEntities={setSelectedFields}>
      <MenuItem onSelect={toggleResetModal}>Reset</MenuItem>
      {showResetModal && (
        <IndexSetCustomFieldTypeRemoveModal show
                                            fields={selectedFields}
                                            onClose={toggleResetModal}
                                            indexSetIds={[indexSetId]}
                                            setSelectedFields={setSelectedFields} />
      )}
    </BulkActionsDropdown>
  );
};

export default BulkActions;
