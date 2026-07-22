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

import type { InputSummary } from 'hooks/usePaginatedInputs';
import { Button } from 'components/bootstrap';
import { IfPermitted, LinkContainer } from 'components/common';
import Routes from 'routing/Routes';
import HideOnCloud from 'util/conditional/HideOnCloud';

type Props = {
  input: InputSummary;
};

const extractorsLink = (input: InputSummary) =>
  input.global || !input.node
    ? Routes.global_input_extractors(input.id)
    : Routes.local_input_extractors(input.node, input.id);

const ExtractorsSectionActions = ({ input }: Props) => (
  <HideOnCloud>
    <IfPermitted permissions={[`inputs:edit:${input.id}`, `input_types:create:${input.type}`]}>
      <LinkContainer to={extractorsLink(input)}>
        <Button bsStyle="link" bsSize="xsmall">
          Manage extractors
        </Button>
      </LinkContainer>
    </IfPermitted>
  </HideOnCloud>
);

export default ExtractorsSectionActions;
