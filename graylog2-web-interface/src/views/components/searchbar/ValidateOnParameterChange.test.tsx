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
import * as Immutable from 'immutable';
import { render, waitFor } from 'wrappedTestingLibrary';
import { Formik, Form } from 'formik';

import ValueParameter from 'views/logic/parameters/ValueParameter';
import ParameterBinding from 'views/logic/parameters/ParameterBinding';

import ValidateOnParameterChange from './ValidateOnParameterChange';

describe('ValidateOnParameterChange', () => {
  type SUTProps = React.ComponentProps<typeof ValidateOnParameterChange> & { onValidate: () => void };
  const SUT = ({ onValidate, parameters }: SUTProps) => (
    <Formik validate={onValidate} onSubmit={() => {}} initialValues={{}} validateOnMount={false}>
      <Form>
        <ValidateOnParameterChange parameters={parameters} />
      </Form>
    </Formik>
  );

  const simpleParameters = Immutable.fromJS({
    foo: ValueParameter.create('source', 'Source parameter', 'The value that source should have', 'string', undefined, false, ParameterBinding.empty()),
  });

  it('should not trigger form validation on mount', async () => {
    const onValidateMock = jest.fn();
    render(<SUT parameters={undefined} onValidate={onValidateMock} />);

    expect(onValidateMock).not.toHaveBeenCalled();
  });

  it('should trigger validation when parameters change', async () => {
    const onValidateMock = jest.fn();
    const { rerender } = render(<SUT parameters={simpleParameters} onValidate={onValidateMock} />);

    const updatedParameters = Immutable.fromJS({
      foo: ValueParameter.create('source', 'Source parameter', 'The value that source should have', 'string', 'default value', false, ParameterBinding.empty()),
    });
    rerender(<SUT parameters={updatedParameters} onValidate={onValidateMock} />);

    await waitFor(() => expect(onValidateMock).toHaveBeenCalledTimes(1));
  });
});
