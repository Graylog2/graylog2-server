import * as React from 'react';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import View from 'views/logic/views/View';

import ViewPropertiesModal from './ViewPropertiesModal';

describe('ViewPropertiesModal', () => {
  it('should use updated view when saving', async () => {
    const onSave = jest.fn();
    const view = View.builder()
      .type(View.Type.Dashboard)
      .title('')
      .build();
    render(<ViewPropertiesModal onClose={jest.fn()} onSave={onSave} title="Saving new dashboard" view={view} show />);

    await screen.findByText('Saving new dashboard');
    const titleInput = await screen.findByRole('textbox', { name: /title/i, hidden: true });

    await userEvent.type(titleInput, 'My title');
    userEvent.click(await screen.findByRole('button', { name: 'Save', hidden: true }));

    await waitFor(() => {
      expect(onSave).toHaveBeenCalledWith(expect.objectContaining({
        title: 'My title',
      }));
    });
  });
});
