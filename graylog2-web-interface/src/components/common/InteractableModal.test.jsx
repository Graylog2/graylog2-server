import React from 'react';
import { render } from 'wrappedTestingLibrary';

import InteractableModal from './InteractableModal';

describe('<InteractableModal />', () => {
  it('properly renders', () => {
    const { firstChild } = render(<InteractableModal><div /></InteractableModal>);

    expect(firstChild).toMatchSnapshot();
  });
});
