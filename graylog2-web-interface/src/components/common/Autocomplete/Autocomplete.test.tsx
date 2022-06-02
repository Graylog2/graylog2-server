import * as React from 'react';
import { Formik } from 'formik';
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';

import Autocomplete from './Autocomplete';

const OPTIONS = [
  { value: 'Rojo', label: 'Red' },
  { value: 'Verde', label: 'Green' },
  { value: 'Amarillo', label: 'Yellow' },
];

describe('Autocomplete component', () => {
  it('should render the field with a label', async () => {
    const { getByText, baseElement } = render(
      <Formik initialValues={{ value: 'Verde', label: 'Green' }}
              onSubmit={(e) => console.log(e)}>
        <Autocomplete fieldName="spaColor"
                      label="Color translator"
                      helpText="Choose a color"
                      options={OPTIONS} />
      </Formik>);

    let label = null;
    let pseudoInput = null;

    await waitFor(() => {
      label = getByText('Color translator');
      pseudoInput = baseElement.querySelector('[id="spaColor"]');
    });

    expect(label).toBeVisible();
    expect(pseudoInput).toBeVisible();
  });

  it('should let the user type', async () => {
    const { baseElement } = render(
      <Formik initialValues={{ value: 'Verde', label: 'Green' }}
              onSubmit={(e) => console.log(e)}>
        <Autocomplete fieldName="spaColor"
                      label="Color translator"
                      helpText="Choose a color"
                      options={OPTIONS} />
      </Formik>);

    fireEvent.change(baseElement.querySelector('input'), { target: { value: 'Naranja' } });

    expect(baseElement).toHaveTextContent('Naranja');
  });

  it('should show a list with options', async () => {
    const { baseElement } = render(
      <Formik initialValues={{ value: 'Verde', label: 'Green' }}
              onSubmit={(e) => console.log(e)}>
        <Autocomplete fieldName="spaColor"
                      label="Color translator"
                      helpText="Choose a color"
                      options={OPTIONS} />
      </Formik>);

    fireEvent.change(baseElement.querySelector('input'), { target: { value: 'ver' } });

    let list = null;

    await waitFor(() => {
      list = baseElement.querySelector('[class$="menu"]');
    });

    expect(list).toBeVisible();
  });
});
