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
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import styled, { css } from 'styled-components';

import { useStore } from 'stores/connect';
import type { IndexSet, IndexSetsStoreState } from 'stores/indices/IndexSetsStore';
import { IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { Spinner } from 'components/common';
import { Alert, BootstrapModalForm, Input, Badge } from 'components/bootstrap';
import type {
  RemovalResponse,
} from 'components/indices/IndexSetFieldTypes/hooks/useRemoveCustomFieldTypeMutation';
import useRemoveCustomFieldTypeMutation from 'components/indices/IndexSetFieldTypes/hooks/useRemoveCustomFieldTypeMutation';
import IndexSetsRemovalErrorAlert from 'components/indices/IndexSetFieldTypes/IndexSetsRemovalErrorAlert';

const StyledLabel = styled.h5`
  font-weight: bold;
  margin-bottom: 5px;
`;

const RedBadge = styled(Badge)(({ theme }) => css`
  background-color: ${theme.colors.variant.light.danger};
`);

const BetaBadge = () => <RedBadge>Beta Feature</RedBadge>;

type Props = {
  show: boolean,
  fields: Array<string>,
  onClose: () => void,
  indexSetIds: Array<string>,
  setSelectedFields: (fields: Array<string>) => void
}

type ContentProps = {
  fields: Array<string>,
  indexSetIds: Array<string>,
  indexSets: Record<string, IndexSet>,
  setRotated: React.Dispatch<React.SetStateAction<boolean>>
  rotated: boolean,
}

const indexSetsStoreMapper = ({ indexSets }: IndexSetsStoreState): Record<string, IndexSet> => {
  if (!indexSets) return null;

  return Object.fromEntries(indexSets.map((indexSet) => ([indexSet.id, indexSet])));
};

const IndexSetCustomFieldTypeRemoveContent = ({ indexSets, fields, setRotated, rotated, indexSetIds }: ContentProps) => {
  const fieldsStr = useMemo(() => fields.join(', '), [fields]);
  const indexSetsStr = useMemo(() => indexSetIds.map((id) => indexSets[id].title).join(', '), [indexSetIds, indexSets]);

  return (
    <div>
      <Alert>
        After removing the custom field type for <b>{fieldsStr}</b> in <b>{indexSetsStr}</b> the settings of your search engine will be used
      </Alert>
      <StyledLabel>Select Rotation Strategy</StyledLabel>
      <p>
        To see and use field type changes for <b>{fieldsStr}</b>, you have to rotate indices. You can automatically rotate affected indices after submitting this form or do that manually later.
      </p>
      <Input type="checkbox"
             id="rotate"
             name="rotate"
             label="Rotate affected indices after change"
             onChange={() => setRotated((cur: boolean) => !cur)}
             checked={rotated} />
    </div>
  );
};

const IndexSetCustomFieldTypeRemoveModal = ({ show, fields, onClose, indexSetIds, setSelectedFields }: Props) => {
  const indexSets = useStore(IndexSetsStore, indexSetsStoreMapper);
  const [removalResponse, setRemovalResponse] = useState<RemovalResponse>(null);
  const [rotated, setRotated] = useState(true);
  const onErrorHandler = useCallback((response: RemovalResponse) => {
    const failedFields = response.flatMap(((indexSet) => indexSet.failures.map(({ entityId }) => entityId)));
    setSelectedFields(failedFields);
    setRemovalResponse(response);
  }, [setSelectedFields]);
  const onSuccessHandler = useCallback(() => {
    onClose();
    setSelectedFields([]);
  }, [onClose, setSelectedFields]);
  const { removeCustomFieldTypeMutation } = useRemoveCustomFieldTypeMutation({ onErrorHandler, onSuccessHandler });
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const telemetryPathName = useMemo(() => getPathnameWithoutId(pathname), [pathname]);
  const onSubmit = useCallback((e: React.FormEvent) => {
    e.preventDefault();
    setRemovalResponse(null);

    removeCustomFieldTypeMutation({ fields, indexSets: indexSetIds, rotated })
      .then(() => {
        sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_FIELD_VALUE_ACTION.REMOVE_CUSTOM_FIELD_TYPE_REMOVED, {
          app_pathname: telemetryPathName,
          app_action_value:
                        {
                          value: 'removed-custom-field-type',
                          rotated,
                        },
        });
      });
  }, [fields, indexSetIds, removeCustomFieldTypeMutation, rotated, sendTelemetry, telemetryPathName]);

  const onCancel = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_FIELD_VALUE_ACTION.REMOVE_CUSTOM_FIELD_TYPE_CLOSED, { app_pathname: telemetryPathName, app_action_value: 'removed-custom-field-type-closed' });
    onClose();
  }, [onClose, sendTelemetry, telemetryPathName]);

  useEffect(() => {
    IndexSetsActions.list(false);
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_FIELD_VALUE_ACTION.REMOVE_CUSTOM_FIELD_TYPE_OPENED, { app_pathname: telemetryPathName, app_action_value: 'removed-custom-field-type-opened' });
  }, [sendTelemetry, telemetryPathName]);

  return (
    <BootstrapModalForm title={<span>Remove Custom Field Type <BetaBadge /></span>}
                        submitButtonText="Remove custom field type"
                        onSubmitForm={onSubmit}
                        onCancel={onCancel}
                        show={show}
                        bsSize="large">
      {!indexSets ? <Spinner />
        : (
          <IndexSetCustomFieldTypeRemoveContent rotated={rotated}
                                                setRotated={setRotated}
                                                indexSetIds={indexSetIds}
                                                fields={fields}
                                                indexSets={indexSets} />
        )}
      {removalResponse && <IndexSetsRemovalErrorAlert removalResponse={removalResponse} indexSets={indexSets} />}
    </BootstrapModalForm>
  );
};

export default IndexSetCustomFieldTypeRemoveModal;
