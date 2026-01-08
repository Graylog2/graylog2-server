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
import { useMemo } from 'react';
import { Formik } from 'formik';
import type { FormikErrors } from 'formik';
import { useParams, useNavigate } from 'react-router-dom';

import Routes from 'routing/Routes';
import { Wizard, Spinner } from 'components/common';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import {
  useFetchLookupTable,
  useCreateLookupTable,
  useUpdateLookupTable,
} from 'components/lookup-tables/hooks/useLookupTablesAPI';
import useSteps from 'components/lookup-tables/lookup-table-form/use-steps';
import type { LookupTable } from 'logic/lookup-tables/types';

import WizardButtons from './wizard-buttons';

export type LookupTableType = LookupTable & {
  enable_single_value: boolean;
  enable_multi_value: boolean;
};

export const INIT_TABLE_VALUES: LookupTableType = {
  id: undefined,
  title: '',
  description: '',
  name: '',
  cache_id: undefined,
  data_adapter_id: undefined,
  enable_single_value: false,
  default_single_value: '',
  default_single_value_type: 'NULL',
  enable_multi_value: false,
  default_multi_value: '',
  default_multi_value_type: 'NULL',
  content_pack: null,
};

const validations = (values: LookupTableType) => {
  const errors: FormikErrors<LookupTableType> = {};

  if (!values.title) errors.title = 'Title is required';
  if (!values.name) errors.name = 'Name is required';
  if (!values.cache_id) errors.cache_id = 'Cache is required';
  if (!values.data_adapter_id) errors.data_adapter_id = 'Data Adapter is required';

  return errors;
};

function LookupTableWizard() {
  const { lutIdOrName } = useParams<{ lutIdOrName: string }>();
  const { lookupTable, loadingLookupTable } = useFetchLookupTable(lutIdOrName);
  const initialValues = useMemo(() => lookupTable || INIT_TABLE_VALUES, [lookupTable]);
  const [steps, { activeStep, setActiveStep }] = useSteps();
  const navigate = useNavigate();
  const sendTelemetry = useSendTelemetry();
  const { createLookupTable, creatingLookupTable } = useCreateLookupTable();
  const { updateLookupTable, updatingLookupTable } = useUpdateLookupTable();

  const isCreate = useMemo(() => !lookupTable, [lookupTable]);

  const handleStepChange = (newStepKey: string) => {
    setActiveStep(newStepKey);
  };

  const handleSubmit = async (values: LookupTableType) => {
    const payload = { ...values };

    delete payload.enable_multi_value;
    delete payload.enable_single_value;

    const promise = isCreate ? createLookupTable(payload) : updateLookupTable(payload);

    return promise.then(() => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.LUT[isCreate ? 'CREATED' : 'UPDATED'], {
        app_pathname: 'lut',
        app_section: 'lookup_table',
        event_details: {
          lookup_table_name: values.name,
        },
      });

      navigate(Routes.SYSTEM.LOOKUPTABLES.OVERVIEW);
    });
  };

  if (loadingLookupTable) return <Spinner text="Loading Lookup Table ..." />;

  return (
    <Formik initialValues={initialValues} onSubmit={handleSubmit} validate={validations} validateOnMount>
      <>
        <Wizard
          steps={steps}
          activeStep={activeStep}
          onStepChange={handleStepChange}
          horizontal
          justified
          containerClassName="flex-row"
          hidePreviousNextButtons
        />
        <WizardButtons
          isCreate={!lookupTable}
          stepIds={steps.map((step) => step.key)}
          activeStepId={activeStep}
          onStepChange={handleStepChange}
          isLoading={creatingLookupTable || updatingLookupTable}
        />
      </>
    </Formik>
  );
}

export default LookupTableWizard;
