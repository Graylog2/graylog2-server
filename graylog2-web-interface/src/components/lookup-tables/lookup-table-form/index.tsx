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
import { Formik } from 'formik';
import { useParams } from 'react-router-dom';

import { Wizard, Spinner } from 'components/common';
import { INIT_TABLE_VALUES } from 'components/lookup-tables/LookupTableForm';
import { useFetchLookupTable } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import useSteps from 'components/lookup-tables/lookup-table-form/use-steps';
import type { LookupTableType } from 'components/lookup-tables/LookupTableForm';

import WizardButtons from './wizard-buttons';

function LookupTableWizard() {
  const { lutIdOrName } = useParams<{ lutIdOrName: string }>();
  const { lookupTable, loadingLookupTable } = useFetchLookupTable(lutIdOrName);
  const initialValues = React.useMemo(() => lookupTable || INIT_TABLE_VALUES, [lookupTable]);
  const [steps, { activeStep, setActiveStep }] = useSteps();

  const handleStepChange = (newStepKey: string) => {
    setActiveStep(newStepKey);
  };

  const handleSubmit = (values: LookupTableType) => {
    console.log(values);
  };

  if (loadingLookupTable) return <Spinner text="Loading Lookup Table ..." />;

  return (
    <Formik initialValues={initialValues} onSubmit={handleSubmit}>
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
        />
      </>
    </Formik>
  );
}

export default LookupTableWizard;
