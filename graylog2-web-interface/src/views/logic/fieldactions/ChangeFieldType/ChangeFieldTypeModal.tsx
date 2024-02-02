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
import useFiledTypes from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes';
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
import useInitialSelection from 'views/logic/fieldactions/ChangeFieldType/hooks/useInitialSelection';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

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

const FailureStreamLink = () => {
  const { data: failureStream, isFetching: isFetchingFailureStream, isError: isErrorFailureStream } = useStream(failureStreamId);
  if (isFetchingFailureStream) return <Spinner />;

  return (
    <span>
      <StreamLink stream={isErrorFailureStream ? { id: failureStreamId, title: 'Processing and Indexing Failures' } : failureStream} />
      <i> (<Link to={Routes.SYSTEM.ENTERPRISE}>Enterprise Plugin</Link> required)</i>
    </span>
  );
};

const ChangeFieldTypeModal = ({ show, onClose, field }: Props) => {
  const sendTelemetry = useSendTelemetry();
  const [rotated, setRotated] = useState(false);
  const [newFieldType, setNewFieldType] = useState(null);
  const { data: { fieldTypes }, isLoading: isOptionsLoading } = useFiledTypes();
  const fieldTypeOptions = useMemo(() => Object.entries(fieldTypes)
    .sort(([, label1], [, label2]) => defaultCompare(label1, label2))
    .map(([value, label]) => ({
      value,
      label,
    })), [fieldTypes]);

  const [indexSetSelection, setIndexSetSelection] = useState<Array<string>>();

  const { putFiledTypeMutation } = usePutFiledTypeMutation();
  const initialSelection = useInitialSelection();
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
            isAllIndexesSelected: indexSetSelection.length === initialSelection.length,
          },
      });

      onClose();
    });
  }, [field, indexSetSelection, initialSelection.length, newFieldType, onClose, putFiledTypeMutation, rotated, sendTelemetry, telemetryPathName]);

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

  return (
    <BootstrapModalForm title={<span>Change {field} field type <BetaBadge /></span>}
                        submitButtonText="Change field type"
                        onSubmitForm={onSubmit}
                        onCancel={onCancel}
                        show={show}
                        bsSize="large">
      <div>
        <Alert bsStyle="warning">
          Changing the type of the field can have a significant impact on the ingestion of future log messages.
          If you declare a field to have a type which is incompatible with the logs you are ingesting, it can lead to
          ingestion errors. It is recommended to enable <DocumentationLink page={DocsHelper.PAGES.INDEXER_FAILURES} displayIcon text="Failure Processing" /> and watch
          the <FailureStreamLink /> stream closely afterwards.
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
        <IndexSetsTable field={field} setIndexSetSelection={setIndexSetSelection} fieldTypes={fieldTypes} initialSelection={initialSelection} />
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
