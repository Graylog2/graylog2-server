import React from 'react';
import { render, fireEvent } from 'wrappedTestingLibrary';

import EventListConfiguration from './EventListConfiguration';

describe('EventListConfiguration', () => {
  it('should render minimal', () => {
    const { container } = render(<EventListConfiguration />);

    expect(container).toMatchSnapshot();
  });

  it('should fire event onClick', () => {
    const onChange = jest.fn();
    const { getByText } = render(<EventListConfiguration onChange={onChange} />);
    const checkbox = getByText('Enable Event Annotation');

    fireEvent.click(checkbox);

    expect(onChange).toHaveBeenCalledTimes(1);
  });
});
