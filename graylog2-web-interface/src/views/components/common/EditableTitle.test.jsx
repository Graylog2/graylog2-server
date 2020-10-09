import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import { fireEvent } from '@testing-library/dom';
import EditableTitle from './EditableTitle';

describe('EditableTitle', () => {
  it('stops submit event propagation', () => {
    const onSubmit = jest.fn((e) => e.persist());
    render((
      <div onSubmit={onSubmit}>
        <EditableTitle value="Current title" onChange={jest.fn()} />
      </div>
    ));

    const currentTitle = screen.getByText('Current title');
    fireEvent.dblClick(currentTitle);

    const titleInput = screen.getByRole('textbox');
    fireEvent.change(titleInput, { target: { value: 'New title' } });
    fireEvent.submit(titleInput);

    expect(onSubmit).not.toHaveBeenCalled();
  });
});
