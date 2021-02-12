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
import { render, screen } from 'wrappedTestingLibrary';
import { Formik } from 'formik';

import HighlightingColorForm from 'views/components/sidebar/highlighting/HighlightingColorForm';
import { GradientColor, StaticColor } from 'views/logic/views/formatting/highlighting/HighlightingColor';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType, { Properties } from 'views/logic/fieldtypes/FieldType';

describe('HighlightingColorForm', () => {
  const field = FieldTypeMapping.create('foo', FieldType.create('number', [Properties.Numeric]));

  // eslint-disable-next-line react/prop-types
  const SimpleForm = ({ value = undefined, ...rest }) => (
    <Formik initialValues={{ color: value }} onSubmit={() => {}}>
      <HighlightingColorForm field={field} {...rest} />
    </Formik>
  );

  it('selects correct type for static color', async () => {
    const staticColor = StaticColor.create('#666666');
    render(<SimpleForm value={staticColor} />);

    const staticOption = await screen.findByLabelText('Static Color');

    expect(staticOption).toBeChecked();
  });

  it('selects correct type for gradient', async () => {
    const gradientColor = GradientColor.create('Viridis', 0, 100);
    render(<SimpleForm value={gradientColor} />);

    const gradientOption = await screen.findByLabelText('Gradient');

    expect(gradientOption).toBeChecked();

    await screen.findByText('Viridis');
  });

  it('should not select a type if none is present', async () => {
    render(<SimpleForm />);
    const staticOption = await screen.findByLabelText('Static Color');

    expect(staticOption).not.toBeChecked();

    const gradientOption = await screen.findByLabelText('Gradient');

    expect(gradientOption).not.toBeChecked();
  });

  it('disables gradient option for non-numeric types', async () => {
    const nonNumericField = FieldTypeMapping.create('foo', FieldType.create('string', []));

    render(<SimpleForm field={nonNumericField} />);
    const staticOption = await screen.findByLabelText('Static Color');

    expect(staticOption).not.toBeDisabled();

    const gradientOption = await screen.findByLabelText('Gradient');

    expect(gradientOption).toBeDisabled();
  });
});
