// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { useCallback } from 'react';
import { Form, Formik } from 'formik';
import { isFunction } from 'lodash';
import type { FormikProps } from 'formik/@flow-typed';

import type { TimeRange } from 'views/logic/queries/Query';
import { onInitializingTimerange, onSubmittingTimerange } from 'views/components/TimerangeForForm';

type Values = {
  timerange: TimeRange,
  streams: Array<string>,
  queryString: string,
};

type Props = {
  initialValues: Values,
  onSubmit: (Values) => void | Promise<any>,
  children: ((props: FormikProps<Values>) => React$Node) | React$Node,
};

const SearchBarForm = ({ initialValues, onSubmit, children }: Props) => {
  const _onSubmit = useCallback(({ timerange, streams, queryString }) => {
    const newTimerange = onSubmittingTimerange(timerange);
    return onSubmit({
      timerange: newTimerange,
      streams,
      queryString,
    });
  }, [onSubmit]);
  const { timerange, streams, queryString } = initialValues;
  const _initialValues = {
    queryString,
    streams,
    timerange: onInitializingTimerange(timerange),
  };
  return (
    <Formik initialValues={_initialValues}
            enableReinitialize
            onSubmit={_onSubmit}>
      {(...args) => (
        <Form>
          {isFunction(children) ? children(...args) : children}
        </Form>
      )}
    </Formik>
  );
};

SearchBarForm.propTypes = {
  initialValues: PropTypes.shape({
    timerange: PropTypes.object.isRequired,
    queryString: PropTypes.string.isRequired,
    streams: PropTypes.arrayOf(PropTypes.string).isRequired,
  }).isRequired,
  onSubmit: PropTypes.func.isRequired,
};

export default SearchBarForm;
