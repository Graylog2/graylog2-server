// @flow strict
import * as React from 'react';
import { render, cleanup, fireEvent, waitForElement } from '@testing-library/react';

import CopyToDashboardForm from './CopyToDashboardForm';

describe('CopyToDashboardForm', () => {
  afterEach(cleanup);

  it('should render the modal minimal', () => {
    const { baseElement } = render(<CopyToDashboardForm />);
    expect(baseElement).toMatchSnapshot();
  });
});
