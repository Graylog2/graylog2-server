// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import { Form, Formik } from 'formik';
import { isFunction } from 'lodash';

import DateTime from 'logic/datetimes/DateTime';
import type { TimeRange } from 'views/logic/queries/Query';
import type { FormikProps } from 'formik/@flow-typed';

const onSubmittingTimerange = (timerange: TimeRange): TimeRange => {
  switch (timerange.type) {
    case 'absolute':
      return {
        type: timerange.type,
        from: DateTime.parseFromString(timerange.from).toISOString(),
        to: DateTime.parseFromString(timerange.to).toISOString(),
      };
    case 'relative':
      return {
        type: timerange.type,
        range: timerange.range,
      };
    case 'keyword':
      return timerange;
    default: throw new Error(`Invalid time range type: ${timerange.type}`);
  }
};

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
  return (
    <Formik initialValues={initialValues}
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

SearchBarForm.propTypes = {};

export default SearchBarForm;
