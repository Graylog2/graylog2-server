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
import last from 'lodash/last';

import { Badge, Alert, Input, Modal } from 'components/bootstrap';
import { Select, Spinner, Wizard } from 'components/common';
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
import useChangeFieldTypeContext from 'views/logic/fieldactions/ChangeFieldType/hooks/useChangeFieldTypeContext';
import ChangeFieldTypeContext from 'views/logic/fieldactions/ChangeFieldType/ChangeFieldTypeContext';
import { Button } from 'preflight/components/common';
import ModalSubmit from 'components/common/ModalSubmit';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import { useStore } from 'stores/connect';
import type { IndexSetsStoreState } from 'stores/indices/IndexSetsStore';
import { IndexSetsActions, IndexSetsStore } from 'stores/indices/IndexSetsStore';

const STEP_KEYS = ['select-new-type', 'select-indexes', 'do-rotation', 'summary'];

const ButtonContainer = styled.div`
  display: flex;
  justify-content: space-between;
`;

const RotationButtons = styled.div`
  display: flex;
  gap: 5px;
`;
const StyledSelect = styled(Select)`
  width: 400px;
  margin-bottom: 20px;
`;

const RedBadge = styled(Badge)(({ theme }) => css`
  background-color: ${theme.colors.variant.light.danger};
`);

const BetaBadge = () => <RedBadge>Beta Feature</RedBadge>;

const failureStreamId = '000000000000000000000004';

const SelectNewType = () => {
  const { onChangeFieldType, newFieldType, field } = useChangeFieldTypeContext();
  const { data: { fieldTypes }, isLoading: isOptionsLoading } = useFiledTypes();
  const fieldTypeOptions = useMemo(() => Object.entries(fieldTypes)
    .sort(([, label1], [, label2]) => defaultCompare(label1, label2))
    .map(([value, label]) => ({
      value,
      label,
    })), [fieldTypes]);
  const { data: failureStream, isFetching: failureStreamLoading } = useStream(failureStreamId);

  return (
    <>
      <Alert bsStyle="warning">
        Changing the type of the field <b>{field}</b> can have a significant impact on the ingestion of future log messages.
        If you declare a field to have a type which is incompatible with the logs you are ingesting, it can lead to
        ingestion errors. It is recommended to enable <DocumentationLink page={DocsHelper.PAGES.INDEXER_FAILURES} displayIcon text="Failure Processing" /> and watch
        the {failureStreamLoading ? <Spinner /> : <StreamLink stream={failureStream} />} stream closely afterwards.
      </Alert>
      <Input label={`New Field Type For ${field}:`} id="new-field-type">
        <StyledSelect inputId="field_type"
                      options={fieldTypeOptions}
                      value={newFieldType}
                      onChange={onChangeFieldType}
                      placeholder="Select field type"
                      disabled={isOptionsLoading}
                      inputProps={{ 'aria-label': 'Select field type' }}
                      required />
      </Input>
    </>
  );
};

const DoRotation = () => {
  const { field, newFieldType } = useChangeFieldTypeContext();

  return (
    <Alert bsStyle="info">
      To see and use the <b>{newFieldType}</b> as a field type for <b>{field}</b>, you have to rotate indices. You can automatically rotate affected indices after submitting this form or do that manually later.
    </Alert>
  );
};

const SelectIndexes = () => {
  const { field, setIndexSetSelection, newFieldType, fieldTypes, initialSelection } = useChangeFieldTypeContext();

  return (
    <>
      <Alert bsStyle="info">
        By default the <b>{newFieldType}</b> as a field type for <b>{field}</b> will be changed in all index sets of the current message/search. You can select for which index sets you would like to make the change.
      </Alert>
      <IndexSetsTable field={field} setIndexSetSelection={setIndexSetSelection} fieldTypes={fieldTypes} initialSelection={initialSelection} />
    </>
  );
};

const indexSetsStoreMapper = ({ indexSets }: IndexSetsStoreState) => indexSets.reduce((res, indexSet) => {
  res[indexSet.id] = indexSet;

  return res;
}, {});

const Summary = () => {
  const { field, rotated, newFieldType, indexSetSelection } = useChangeFieldTypeContext();
  const indexSets = useStore(IndexSetsStore, indexSetsStoreMapper);
  const indexSetSelectionStr = useMemo(() => indexSetSelection.map((id) => indexSets[id].title).join(', '), [indexSetSelection, indexSets]);
  const rotationStr = useMemo(() => `with${rotated ? '' : 'out'} automatically indices rotation`, [rotated]);

  return (
    <p>
      By this action you will set <b>{newFieldType}</b> as a field type for <b>{field}</b> in <b>{indexSetSelectionStr}</b> <span>{rotationStr}</span>
    </p>
  );
};

const Buttons = () => {
  const { activeStep, onCancel, onSubmit, setActiveStep, setRotated } = useChangeFieldTypeContext();
  const activeStepIndex = STEP_KEYS.indexOf(activeStep);
  const showSubmitButton = activeStep === last(STEP_KEYS);

  const handlePreviousClick = useCallback(() => {
    const previousStep = activeStepIndex > 0 ? STEP_KEYS[activeStepIndex - 1] : undefined;
    setActiveStep(previousStep);
  }, [activeStepIndex, setActiveStep]);

  const handleNextClick = useCallback(() => {
    const nextStep = STEP_KEYS[activeStepIndex + 1];
    setActiveStep(nextStep);
  }, [activeStepIndex, setActiveStep]);

  const showRotationButtons = activeStep === 'do-rotation';

  const handleWithRotation = useCallback(() => {
    setRotated(true);
    handleNextClick();
  }, [handleNextClick, setRotated]);

  const handleWithoutRotation = useCallback(() => {
    setRotated(false);
    handleNextClick();
  }, [handleNextClick, setRotated]);

  return (
    <ButtonContainer>
      <Button bsStyle="info"
              onClick={handlePreviousClick}
              disabled={activeStepIndex === 0}>
        Previous
      </Button>
      {
    showSubmitButton && (
      <ModalSubmit onCancel={onCancel}
                   onSubmit={onSubmit}
                   submitButtonText="Change field type" />
    )
}
      {showRotationButtons && (
        <RotationButtons>
          <Button onClick={handleWithoutRotation}>
            Skip rotation
          </Button>
          <Button bsStyle="info"
                  onClick={handleWithRotation}>
            Do rotation
          </Button>
        </RotationButtons>
      )}
      {!showRotationButtons && !showSubmitButton && (
      <Button bsStyle="info"
              onClick={handleNextClick}>
        Next
      </Button>
      )}
    </ButtonContainer>
  );
};

type Props = {
  show: boolean,
  field: string,
  onClose: () => void
}

const steps = [
  {
    key: STEP_KEYS[0],
    title: 'Select new type',
    component: <SelectNewType />,
  },
  {
    key: STEP_KEYS[1],
    title: 'Select indices',
    component: <SelectIndexes />,
  },
  {
    key: STEP_KEYS[2],
    title: 'Rotation',
    component: <DoRotation />,
  },
  {
    key: STEP_KEYS[3],
    title: 'Summary',
    component: <Summary />,
  },
];

const ChangeFieldTypeModal = ({ show, onClose, field }: Props) => {
  const sendTelemetry = useSendTelemetry();
  const [activeStep, setActiveStep] = useState(STEP_KEYS[0]);
  const [rotated, setRotated] = useState(false);
  const [newFieldType, setNewFieldType] = useState(null);
  const { data: { fieldTypes } } = useFiledTypes();

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
    IndexSetsActions.list(false);
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_FIELD_VALUE_ACTION.CHANGE_FIELD_TYPE_OPENED, { app_pathname: telemetryPathName, app_action_value: 'change-field-type-opened' });
  }, [sendTelemetry, telemetryPathName]);

  const onCancel = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_FIELD_VALUE_ACTION.CHANGE_FIELD_TYPE_CLOSED, { app_pathname: telemetryPathName, app_action_value: 'change-field-type-closed' });
    onClose();
  }, [onClose, sendTelemetry, telemetryPathName]);

  const handleStepChange = useCallback((nextStep: string) => {
    setActiveStep(nextStep);
  }, []);

  const contextValue = useMemo(() => (
    {
      onChangeFieldType,
      newFieldType,
      rotated,
      setRotated,
      field,
      setIndexSetSelection,
      indexSetSelection,
      fieldTypes,
      initialSelection,
      activeStep,
      setActiveStep,
      onSubmit,
      onCancel,
    }
  ), [activeStep, field, fieldTypes, indexSetSelection, initialSelection, newFieldType, onCancel, onChangeFieldType, onSubmit, rotated]);

  return (
    <ChangeFieldTypeContext.Provider value={contextValue}>
      <BootstrapModalWrapper showModal={show}
                             onHide={onCancel}
                             backdrop
                             bsSize="large">
        <form>
          <Modal.Header closeButton>
            <Modal.Title><span>Change {field} field type <BetaBadge /></span></Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <Wizard steps={steps}
                    activeStep={activeStep}
                    onStepChange={handleStepChange}
                    horizontal
                    containerClassName=""
                    hidePreviousNextButtons
                    justified />
          </Modal.Body>
          <Modal.Footer>
            <Buttons />
          </Modal.Footer>
        </form>
      </BootstrapModalWrapper>
    </ChangeFieldTypeContext.Provider>
  );
};

export default ChangeFieldTypeModal;
