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
import styled from 'styled-components';

import type { Output } from 'stores/outputs/OutputsStore';
import type { ConfigurationFormData } from 'components/configurationforms';

import EditOutputButton from './EditOutputButton';
import RemoveOutputButton from './RemoveOutputButton';

import type { AvailableOutputRequestedConfiguration } from '../../useAvailableOutputTypes';

type Props = {
  output: Output,
  streamId: string,
  isLoadingOutputTypes: boolean,
  onUpdate: (output: Output, data: ConfigurationFormData<Output['configuration']>) => void,
  getTypeDefinition: (type: string) => undefined | AvailableOutputRequestedConfiguration,
};

const ActionButtonsWrap = styled.span`
  margin-right: 6px;
  float: right;
`;

const OutputItem = ({ output, streamId, isLoadingOutputTypes, onUpdate, getTypeDefinition }: Props) => (
  <tr>
    <td>{output.title} <small>{`(Type: ${output.type})`}</small> </td>
    {/* eslint-disable-next-line jsx-a11y/control-has-associated-label */}
    <td>
      <ActionButtonsWrap className="align-right">
        <EditOutputButton disabled={isLoadingOutputTypes}
                          output={output}
                          onUpdate={onUpdate}
                          getTypeDefinition={getTypeDefinition} />
        <RemoveOutputButton output={output} streamId={streamId} />
      </ActionButtonsWrap>
    </td>
  </tr>
);

export default OutputItem;
