import { useContext, useEffect } from 'react';
import { useFormikContext } from 'formik';

import { ValidationStateContext } from '../widgets/EditWidgetFrame';

const PropagateValidationState = () => {
  const { isValid } = useFormikContext();
  const [, setHasErrors] = useContext(ValidationStateContext);

  useEffect(() => setHasErrors(!isValid), [isValid, setHasErrors]);

  return null;
};

export default PropagateValidationState;
