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

import React from 'react';

import { ConfigurationWell } from 'components/configurationforms';
import type { InputSummary } from 'hooks/usePaginatedInputs';
import InputStaticFields from 'components/inputs/InputStaticFields';
import type { InputTypeDescriptionsResponse } from 'hooks/useInputTypesDescriptions';

type Props = {
  input: InputSummary;
  inputTypeDescriptions: InputTypeDescriptionsResponse;
};

const ExpandedTitleSection = ({ input, inputTypeDescriptions }: Props) => {
  const definition = inputTypeDescriptions[input.type] as any;

  return (
    <div>
      <span>
        {' '}
        {input.name} &nbsp; ({input.id})
      </span>
      <ConfigurationWell id={input.id} configuration={input.attributes} typeDefinition={definition} />
      <InputStaticFields input={input} />
    </div>
  );
};

export default ExpandedTitleSection;
