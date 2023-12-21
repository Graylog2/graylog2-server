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
import styled from 'styled-components';

import { Button } from 'components/bootstrap';

type Props = {
    onStepComplete: () => void,
};
const Headline = styled.h2`
  margin-top: 5px;
  margin-bottom: 10px;
`;

const MigrationHelpStep = ({ onStepComplete }: Props) => (
  <>
    <Headline>Migration to Datanode !</Headline>
    <p>
      It looks like you updated Graylog and want to configure a data node.<br />
      Data nodes allow you to index and search through all the messages in your Graylog message database.<br />
    </p>
    <p>
      Using this migration tool you can check the compatibility and migrate your exsisting Opensearch data to a Datanode.<br />
    </p>
    <p>
      <Button bsStyle="success" onClick={() => onStepComplete()}>Start Migration</Button>
    </p>
  </>
);
export default MigrationHelpStep;
