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

import React, { useRef } from 'react';
import { screen, render, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import ConfigurationForm from './ConfigurationForm';

describe('ConfigurationForm', () => {
  const SUT = ({ submitAction }: { submitAction: () => void }) => {
    const formRef = useRef(undefined);
    const openForm = () => formRef?.current?.open();

    return (
      <>
        <ConfigurationForm submitAction={submitAction}
                           title="Edit entity"
                           titleValue="Entity title"
                           ref={formRef}
                           submitButtonText="Update entity"
                           typeName="placeholder"
                           cancelAction={() => {}}
                           values={{}} />
        <button type="button" onClick={openForm}>Open modal</button>
      </>
    );
  };

  it('should close modal on save', async () => {
    const submitAction = jest.fn();

    render(<SUT submitAction={submitAction} />);

    userEvent.click(await screen.findByRole('button', {
      name: /open modal/i,
    }));

    await screen.findByRole('heading', {
      name: /edit entity/i,
      hidden: true,
    });

    userEvent.click(await screen.findByRole('button', {
      name: /update entity/i,
      hidden: true,
    }));

    await waitFor(() => expect(submitAction).toHaveBeenCalledTimes(1));

    await waitFor(() => expect(screen.queryByRole('heading', {
      name: /edit entity/i,
      hidden: true,
    })).not.toBeInTheDocument());
  });
});
