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

import { Alert, Button, Input, Modal } from 'components/bootstrap';
import { ModalSubmit, Select } from 'components/common';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useIndexSetsList from 'components/indices/hooks/useIndexSetsList';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import useFetchIndexSetFieldTypes from 'components/indices/IndexSetFieldTypeProfiles/hooks/useFetchIndexSetFieldTypes';
import buildImportedMappings, {
  IMPORT_ORIGIN_KEYS,
  IMPORT_ORIGIN_LABELS,
} from 'components/indices/IndexSetFieldTypeProfiles/helpers/importProfileMappings';
import type { ImportOriginKey } from 'components/indices/IndexSetFieldTypeProfiles/helpers/importProfileMappings';

type Props = {
  onClose: () => void;
};

const ImportProfileModal = ({ onClose }: Props) => {
  const [indexSetId, setIndexSetId] = useState<string | null>(null);
  const [selectedOrigins, setSelectedOrigins] = useState<Array<ImportOriginKey>>(IMPORT_ORIGIN_KEYS);
  const [nothingToImport, setNothingToImport] = useState(false);
  const { pushWithState } = useHistory();
  const sendTelemetry = useSendTelemetry();
  const {
    data: { indexSets },
  } = useIndexSetsList();
  const {
    data: { fieldTypes },
    isLoading: fieldTypesLoading,
  } = useFieldTypesForMappings();
  const { fetchFieldTypes, isLoading, isError, reset } = useFetchIndexSetFieldTypes();

  const indexSetOptions = indexSets.map(({ id, title }) => ({ value: id, label: title }));

  const resetResult = () => {
    setNothingToImport(false);
    reset();
  };

  const onIndexSetChange = (newIndexSetId: string) => {
    setIndexSetId(newIndexSetId);
    resetResult();
  };

  const onOriginToggle = (origin: ImportOriginKey) => {
    setSelectedOrigins((cur) => (cur.includes(origin) ? cur.filter((key) => key !== origin) : [...cur, origin]));
    resetResult();
  };

  const importProfile = () => {
    setNothingToImport(false);

    fetchFieldTypes(indexSetId, {
      onSuccess: (fields) => {
        const { customFieldMappings, skippedFieldsCount } = buildImportedMappings(
          fields,
          selectedOrigins,
          Object.keys(fieldTypes),
        );

        if (customFieldMappings.length === 0) {
          setNothingToImport(true);

          return;
        }

        sendTelemetry(TELEMETRY_EVENT_TYPE.INDEX_SET_FIELD_TYPE_PROFILE.IMPORT_RAN, {
          app_action_value: { mappingsQuantity: customFieldMappings.length, skippedFieldsCount },
        });

        pushWithState(Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.CREATE, { customFieldMappings, skippedFieldsCount });
      },
    });
  };

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    importProfile();
  };

  return (
    <Modal show onHide={onClose}>
      <form onSubmit={onSubmit}>
        <Modal.Header>
          <Modal.Title>Import profile from index set</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Input id="import_profile_index_set" label="Index set">
            <Select
              inputId="import_profile_index_set"
              options={indexSetOptions}
              value={indexSetId}
              onChange={onIndexSetChange}
              placeholder="Select index set"
              required
            />
          </Input>
          <p>Include field types from:</p>
          {IMPORT_ORIGIN_KEYS.map((origin) => (
            <Input
              key={origin}
              type="checkbox"
              id={`import_profile_origin_${origin}`}
              label={IMPORT_ORIGIN_LABELS[origin]}
              checked={selectedOrigins.includes(origin)}
              onChange={() => onOriginToggle(origin)}
            />
          ))}
          {isError && (
            <Alert bsStyle="danger">
              Could not load the fields of this index set.{' '}
              <Button bsSize="xs" onClick={importProfile}>
                Retry
              </Button>
            </Alert>
          )}
          {nothingToImport && <Alert bsStyle="info">No fields found for the selected origins</Alert>}
        </Modal.Body>
        <Modal.Footer>
          <ModalSubmit
            submitButtonText="Import"
            submitLoadingText="Importing..."
            submitButtonType="submit"
            onCancel={onClose}
            disabledSubmit={!indexSetId || selectedOrigins.length === 0 || fieldTypesLoading}
            isAsyncSubmit
            isSubmitting={isLoading}
          />
        </Modal.Footer>
      </form>
    </Modal>
  );
};

export default ImportProfileModal;
