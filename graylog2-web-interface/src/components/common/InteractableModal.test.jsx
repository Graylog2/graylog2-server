import React from 'react';
import { render } from '@testing-library/react';

import InteractableModal from './InteractableModal';

describe('<InteractableModal />', () => {
  it('properly renders', () => {
    const { firstChild } = render(<InteractableModal><div /></InteractableModal>);

    expect(firstChild).toMatchSnapshot();
  });
});
