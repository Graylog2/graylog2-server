import { useEffect } from 'react';
import { useFormikContext } from 'formik';

const useSyncSearchBarFormErrors = ({ queryString, filter, validationStatus }) => {
  const { errors, setFieldError } = useFormikContext<{ queryString: string }>();

  useEffect(() => {
    if ((queryString || filter) && !errors.queryString && validationStatus === 'ERROR') {
      setFieldError('queryString', 'query validation error');
    } else if (errors.queryString && ((!queryString && !filter) || (!validationStatus || validationStatus === 'OK'))) {
      setFieldError('queryString', undefined);
    }
  }, [queryString, filter, errors, validationStatus, setFieldError]);
};

export default useSyncSearchBarFormErrors;
