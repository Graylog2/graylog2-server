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

import { Button, Input } from 'components/bootstrap';

import type { RuleBuilderRule } from './types';

import PipelinesUsingRule from '../PipelinesUsingRule';

type Props = {
  rule: RuleBuilderRule|null,
  onSave: (rule: Partial<RuleBuilderRule>) => void,
};

const RuleBuilderForm = ({ rule, onSave }: Props) => {
  const [title, setTitle] = useState<string>('');
  const [description, setDescription] = useState<string>('');

  const handleTitleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setTitle(event.target.value);
  };

  const handleDescriptionChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setDescription(event.target.value);
  };

  const handleSave = () => {
    onSave({
      ...rule,
      title,
      description,
    });
  };

  return (
    <>
      <fieldset>
        <Input type="text"
               id="title"
               label="Title"
               value={title}
               onChange={handleTitleChange}
               autoFocus
               help="Rule title." />

        <Input type="textarea"
               id="description"
               label="Description"
               value={description}
               onChange={handleDescriptionChange}
               autoFocus
               help="Rule description (optional)." />

        <PipelinesUsingRule create={Boolean(rule)} />
      </fieldset>

      <Button type="button" bsStyle="success" onClick={handleSave}>
        Save
      </Button>
    </>
  );
};

export default RuleBuilderForm;
