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

import BootstrapModalConfirm from './BootstrapModalConfirm';

jest.mock('./BootstrapModalWrapper', () => ({ bsSize = undefined }: { bsSize?: string }) => (
  <div>
    <span data-testid="bsSize">{bsSize ?? 'undefined'}</span>
  </div>
));

describe('BootstrapModalConfirm', () => {
  const SUT = (props: Pick<React.ComponentProps<typeof BootstrapModalConfirm>, 'size'> = {}) => (
    <BootstrapModalConfirm showModal title="Test Modal" onCancel={jest.fn()} onConfirm={jest.fn()} {...props}>
      <span>Modal content</span>
    </BootstrapModalConfirm>
  );

  it('forwards size prop as bsSize to BootstrapModalWrapper', async () => {
    render(<SUT size="lg" />);

    expect(await screen.findByTestId('bsSize')).toHaveTextContent('lg');
  });

  it('passes undefined bsSize when no size is provided', async () => {
    render(<SUT />);

    expect(await screen.findByTestId('bsSize')).toHaveTextContent('undefined');
  });
});
