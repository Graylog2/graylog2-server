import type * as Immutable from 'immutable';
import { useFormikContext } from 'formik';
import { useEffect, useRef } from 'react';

import type { ParameterBindings } from 'views/logic/search/SearchExecutionState';
import type Parameter from 'views/logic/parameters/Parameter';

type Props = {
  parameters: Immutable.Set<Parameter> | undefined,
  parameterBindings: ParameterBindings | undefined,
}

const ValidateFormOnParameterChange = ({ parameterBindings, parameters }: Props) => {
  const { validateForm } = useFormikContext();
  const mounted = useRef(false);

  useEffect(() => {
    if (mounted.current) {
      validateForm();
    } else {
      mounted.current = true;
    }
  }, [validateForm, parameterBindings, parameters]);

  return null;
};

export default ValidateFormOnParameterChange;
