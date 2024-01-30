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

import React, { useState } from 'react';

import { Button } from 'components/bootstrap';
import { HoverForHelp } from 'components/common';
import IndexSetCustomFieldTypeRemoveModal from 'components/indices/IndexSetFieldTypes/IndexSetCustomFieldTypeRemoveModal';
import ChangeFieldTypeModal from 'views/logic/fieldactions/ChangeFieldType/ChangeFieldTypeModal';
import hasOverride from 'components/indices/helpers/hasOverride';
import type { IndexSetFieldType } from 'components/indices/IndexSetFieldTypes/types';

type Props = {
  fieldType: IndexSetFieldType,
  indexSetId: string,
  refetchFieldTypes: () => void,
}

const FieldTypeActions = ({ fieldType, indexSetId, refetchFieldTypes }: Props) => {
  const [showResetModal, setShowResetModal] = useState<boolean>(false);
  const [showEditModal, setShowEditModal] = useState<boolean>(false);
  const toggleResetModal = () => setShowResetModal((cur) => !cur);
  const toggleEditModal = () => setShowEditModal((cur) => !cur);
  const showResetButton = hasOverride(fieldType);

  return (
    <>
      <Button onClick={toggleEditModal}
              role="button"
              bsSize="xsmall"
              disabled={fieldType.isReserved}
              title={`Edit field type for ${fieldType.fieldName}`}
              tabIndex={0}>
        Edit {
          fieldType.isReserved && (
            <HoverForHelp displayLeftMargin title="Reserved field is not editable" pullRight={false}>
              We use reserved fields internally and expect a certain structure from them. Changing the field type for
              reserved fields might impact the stability of Graylog
            </HoverForHelp>
          )
      }
      </Button>
      {showResetButton && (
        <Button onClick={toggleResetModal}
                role="button"
                bsSize="xsmall"
                title="Reset custom type"
                tabIndex={0}>
          Reset
        </Button>
      )}
      {showResetModal && (
        <IndexSetCustomFieldTypeRemoveModal show
                                            fields={[fieldType.fieldName]}
                                            onClose={toggleResetModal}
                                            indexSetIds={[indexSetId]} />
      )}
      {showEditModal && (
        <ChangeFieldTypeModal initialSelectedIndexSets={[indexSetId]}
                              initialData={{
                                fieldName: fieldType.fieldName,
                                type: fieldType.type,
                              }}
                              onClose={toggleEditModal}
                              show
                              showSelectionTable={false}
                              onSubmitCallback={refetchFieldTypes}
                              showFieldSelect={false} />
      )}
    </>
  );
};

export default FieldTypeActions;
