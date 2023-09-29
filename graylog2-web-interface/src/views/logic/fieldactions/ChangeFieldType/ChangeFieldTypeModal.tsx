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
import React, { useMemo, useCallback, useState } from 'react';
import styled, { css } from 'styled-components';

import { Badge, BootstrapModalForm, Alert, Input } from 'components/bootstrap';
import { Select, Spinner } from 'components/common';
import StreamLink from 'components/streams/StreamLink';
import useFiledTypes from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes';
import IndexSetsTable from 'views/logic/fieldactions/ChangeFieldType/IndexSetsTable';
import usePutFiledTypeMutation from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeMutation';
import useStream from 'components/streams/hooks/useStream';
import { DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';
import { defaultCompare } from 'logic/DefaultCompare';

const StyledSelect = styled(Select)`
  width: 400px;
  margin-bottom: 20px;
`;

const RedBadge = styled(Badge)(({ theme }) => css`
  background-color: ${theme.colors.variant.light.danger};
`);

const BetaBadge = () => <RedBadge>Beta Feature</RedBadge>;

const failureStreamId = '000000000000000000000004';

type Props = {
  show: boolean,
  field: string,
  onClose: () => void
}

const ChangeFieldTypeModal = ({ show, onClose, field }: Props) => {
  const [rotated, setRotated] = useState(false);
  const [newFieldType, setNewFieldType] = useState(null);
  const { data: { fieldTypes }, isLoading: isOptionsLoading } = useFiledTypes();
  const fieldTypeOptions = useMemo(() => Object.entries(fieldTypes)
    .sort(([, label1], [, label2]) => defaultCompare(label1, label2))
    .map(([value, label]) => ({
      value,
      label,
    })), [fieldTypes]);
  const { data: failureStream, isFetching: failureStreamLoading } = useStream(failureStreamId);

  const [indexSetSelection, setIndexSetSelection] = useState<Array<string>>();

  const { putFiledTypeMutation } = usePutFiledTypeMutation();
  const onSubmit = useCallback((e: React.FormEvent) => {
    e.preventDefault();

    putFiledTypeMutation({
      indexSetSelection,
      newFieldType,
      rotated,
      field,
    }).then(() => onClose());
  }, [field, indexSetSelection, newFieldType, onClose, putFiledTypeMutation, rotated]);

  const onChangeFieldType = useCallback((value: string) => {
    setNewFieldType(value);
  }, []);

  return (
    <BootstrapModalForm title={<span>Change {field} field type <BetaBadge /></span>}
                        submitButtonText="Change field type"
                        onSubmitForm={onSubmit}
                        onCancel={onClose}
                        show={show}
                        bsSize="large">
      <div>
        <Alert bsStyle="warning">
          Changing the type of the field can have a significant impact on the ingestion of future log messages.
          If you declare a field to have a type which is incompatible with the logs you are ingesting, it can lead to
          ingestion errors. It is recommended to enable <DocumentationLink page={DocsHelper.PAGES.INDEXER_FAILURES} displayIcon text="Failure Processing" /> and watch
          the {failureStreamLoading ? <Spinner /> : <StreamLink stream={failureStream} />} stream closely afterwards.
        </Alert>
        <Input label="New Field Type:" id="new-field-type">
          <StyledSelect inputId="field_type"
                        options={fieldTypeOptions}
                        value={newFieldType}
                        onChange={onChangeFieldType}
                        placeholder="Select field type"
                        disabled={isOptionsLoading}
                        inputProps={{ 'aria-label': 'Select field type' }}
                        required />
        </Input>
        <Alert bsStyle="info">
          By default the type will be changed in all index sets of the current message/search. By expanding the next section, you can select for which index sets you would like to make the change.
        </Alert>
        <IndexSetsTable field={field} setIndexSetSelection={setIndexSetSelection} fieldTypes={fieldTypes} />
        <Input type="checkbox"
               id="rotate"
               name="rotate"
               label="Rotate affected indices after change"
               onChange={() => setRotated((cur) => !cur)}
               checked={rotated} />
      </div>
    </BootstrapModalForm>
  );
};

export default ChangeFieldTypeModal;
