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
import React, { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { MemoryRouter } from 'react-router-dom';

import { Button, Input } from 'components/bootstrap';
import { CreateModal, CreatePage } from 'components/common';

type StoryValues = { title: string; description: string };

const FormFields = () => (
  <>
    <Input
      type="text"
      id="title"
      name="title"
      label="Title"
      placeholder="e.g. Production errors"
      labelClassName="col-sm-3"
      wrapperClassName="col-sm-9"
      required
    />
    <Input
      type="text"
      id="description"
      name="description"
      label="Description"
      placeholder="What does this stream collect?"
      labelClassName="col-sm-3"
      wrapperClassName="col-sm-9"
    />
  </>
);

export const CurrentContextModal: StoryObj = {
  tags: ['!dev'],
  render: () => {
    const [show, setShow] = useState(false);

    return (
      <>
        <Button onClick={() => setShow(true)}>Create Stream</Button>
        <CreateModal<StoryValues>
          entityName="Stream"
          show={show}
          onClose={() => setShow(false)}
          initialValues={{ title: '', description: '' }}
          onSubmit={async () => {}}>
          <FormFields />
        </CreateModal>
      </>
    );
  },
};

export const NewContextPage: StoryObj = {
  tags: ['!dev'],
  decorators: [
    (Story) => (
      <MemoryRouter>
        <Story />
      </MemoryRouter>
    ),
  ],
  render: () => (
    <CreatePage<StoryValues>
      entityName="Stream"
      overviewRoute="/streams"
      detailsRoute={(id) => `/streams/${id}`}
      initialValues={{ title: '', description: '' }}
      onSubmit={async () => ({ id: 'new-stream-id' })}
      description="Streams route incoming messages into categories. Route a message into a stream by applying matching rules.">
      <FormFields />
    </CreatePage>
  ),
};

const meta: Meta = {
  title: 'Patterns/Creating an Entity',
  parameters: { layout: 'padded' },
};

export default meta;
