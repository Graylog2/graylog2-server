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
import React, { useMemo, useCallback, useState, useEffect } from 'react';
import styled, { css } from 'styled-components';

import { Badge, BootstrapModalForm, Alert, Input } from 'components/bootstrap';
import { Select, Spinner } from 'components/common';
import StreamLink from 'components/streams/StreamLink';
import IndexSetsTable from 'views/logic/fieldactions/ChangeFieldType/IndexSetsTable';
import usePutFiledTypeMutation from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeMutation';
import useStream from 'components/streams/hooks/useStream';
import { DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';
import { defaultCompare } from 'logic/DefaultCompare';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import type { FieldTypes } from 'views/logic/fieldactions/ChangeFieldType/types';

const StyledSelect = styled(Select)`
  width: 400px;
  margin-bottom: 20px;
`;

const StyledLabel = styled.h5`
  font-weight: bold;
  margin-bottom: 5px;
`;

const RedBadge = styled(Badge)(({ theme }) => css`
  background-color: ${theme.colors.variant.light.danger};
`);

const BetaBadge = () => <RedBadge>Beta Feature</RedBadge>;

const failureStreamId = '000000000000000000000004';

type Props = {
  show: boolean,
  field: string,
  onClose: () => void,
  onSubmitCallback?: () => void,
  initialSelectedIndexSets: Array<string>,
  showSelectionTable?: boolean,
  fieldTypes: FieldTypes,
  isOptionsLoading: boolean,
}

const ChangeFieldTypeModal = ({ show, onSubmitCallback, fieldTypes, isOptionsLoading, initialSelectedIndexSets, onClose, field, showSelectionTable }: Props) => {
  const sendTelemetry = useSendTelemetry();
  const [rotated, setRotated] = useState(true);
  const [newFieldType, setNewFieldType] = useState(null);
  const fieldTypeOptions = useMemo(() => Object.entries(fieldTypes)
    .sort(([, label1], [, label2]) => defaultCompare(label1, label2))
    .map(([value, label]) => ({
      value,
      label,
    })), [fieldTypes]);
  const { data: failureStream, isFetching: failureStreamLoading } = useStream(failureStreamId);

  const [indexSetSelection, setIndexSetSelection] = useState<Array<string>>();

  const { putFiledTypeMutation } = usePutFiledTypeMutation();

  const { pathname } = useLocation();
  const telemetryPathName = useMemo(() => getPathnameWithoutId(pathname), [pathname]);
  const onSubmit = useCallback((e: React.FormEvent) => {
    e.preventDefault();

    putFiledTypeMutation({
      indexSetSelection,
      newFieldType,
      rotated,
      field,
    }).then(() => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_FIELD_VALUE_ACTION.CHANGE_FIELD_TYPE_CHANGED, {
        app_pathname: telemetryPathName,
        app_action_value:
          {
            value: 'change-field-type',
            rotated,
            isAllIndexesSelected: indexSetSelection.length === initialSelectedIndexSets.length,
          },
      });

      onClose();
    }).then(() => onSubmitCallback && onSubmitCallback());
  }, [field, indexSetSelection, initialSelectedIndexSets.length, newFieldType, onClose, onSubmitCallback, putFiledTypeMutation, rotated, sendTelemetry, telemetryPathName]);

  const onChangeFieldType = useCallback((value: string) => {
    setNewFieldType(value);
  }, []);

  useEffect(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_FIELD_VALUE_ACTION.CHANGE_FIELD_TYPE_OPENED, { app_pathname: telemetryPathName, app_action_value: 'change-field-type-opened' });
  }, [sendTelemetry, telemetryPathName]);

  const onCancel = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_FIELD_VALUE_ACTION.CHANGE_FIELD_TYPE_CLOSED, { app_pathname: telemetryPathName, app_action_value: 'change-field-type-closed' });
    onClose();
  }, [onClose, sendTelemetry, telemetryPathName]);

  useEffect(() => {
    setIndexSetSelection(initialSelectedIndexSets);
  }, [initialSelectedIndexSets, setIndexSetSelection]);

  return (
    <BootstrapModalForm title={<span>Change {field} Field Type <BetaBadge /></span>}
                        submitButtonText="Change field type"
                        onSubmitForm={onSubmit}
                        onCancel={onCancel}
                        show={show}
                        bsSize="large">
      <div>
        <Alert bsStyle="warning">
          Changing the type of the field <b>{field}</b> can have a significant impact on the ingestion of future log messages.
          If you declare a field to have a type which is incompatible with the logs you are ingesting, it can lead to
          ingestion errors. It is recommended to enable <DocumentationLink page={DocsHelper.PAGES.INDEXER_FAILURES} displayIcon text="Failure Processing" /> and watch
          the {failureStreamLoading ? <Spinner /> : <StreamLink stream={failureStream} />} stream closely afterwards.
        </Alert>
        <StyledLabel>{`Select Field Type For ${field}`}</StyledLabel>
        <Input id="field_type">
          <StyledSelect inputId="field_type"
                        options={fieldTypeOptions}
                        value={newFieldType}
                        onChange={onChangeFieldType}
                        placeholder="Select field type"
                        disabled={isOptionsLoading}
                        inputProps={{ 'aria-label': `Select Field Type For ${field}` }}
                        required />
        </Input>
        {
          showSelectionTable && (
          <>
        <StyledLabel>Select Targeted Index Sets</StyledLabel>
        <p>
          By default the {newFieldType ? <b>{newFieldType}</b> : 'selected'} field type will be set for the <b>{field}</b> field in all index sets of the current message/search. You can select for which index sets you would like to make the change.
        </p>
        <IndexSetsTable field={field} setIndexSetSelection={setIndexSetSelection} fieldTypes={fieldTypes} initialSelection={initialSelection} />
        <StyledLabel>Select Rotation Strategy</StyledLabel>
        <p>
          To see and use the {newFieldType ? <b>{newFieldType}</b> : 'selected field type'} as a field type for <b>{field}</b>, you have to rotate indices. You can automatically rotate affected indices after submitting this form or do that manually later.
        </p>
          </>
          )
        }
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

ChangeFieldTypeModal.defaultProps = {
  showSelectionTable: true,
  onSubmitCallback: undefined,
};

export default ChangeFieldTypeModal;
