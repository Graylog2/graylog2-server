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

import { Button } from 'components/bootstrap';
import Routes from 'routing/Routes';
import useInputSetupWizard from 'components/inputs/InputSetupWizard/hooks/useInputSetupWizard';

const InputDiagnosisStep = () => {
  const { wizardData: { input } } = useInputSetupWizard();
  return (
    <div>
      <div>Test inputs and parsing without writing any data to the search cluster.</div>
      <br />
      {input?.id && (
        <Button bsSize="xs" onClick={() => window.open(Routes.SYSTEM.INPUT_DIAGNOSIS(input?.id), '_blank')}>
          Go to Input Diagnosis
        </Button>
      )}
    </div>
  )
};

export default InputDiagnosisStep;
