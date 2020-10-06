import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import { fireEvent } from '@testing-library/dom';
import FieldType from 'views/logic/fieldtypes/FieldType';
import PivotConfiguration from './PivotConfiguration';

describe('PivotConfiguration', () => {
  it('stops submit event propagation', () => {
    const onSubmit = jest.fn((e) => e.persist());
    render((
      <div onSubmit={onSubmit}>
        <PivotConfiguration type={FieldType.create('terms')} config={{ limit: 3 }} onClose={jest.fn()} />
      </div>
    ));

    const done = screen.getByRole('button', 'Done');
    fireEvent.click(done);

    expect(onSubmit).not.toHaveBeenCalled();
  });
});
