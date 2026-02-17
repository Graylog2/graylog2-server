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

import type { Stream } from 'stores/streams/StreamsStore';
import { Button } from 'components/bootstrap';
import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import { IfPermitted } from 'components/common';

type Props = {
  stream: Stream;
};

const ExpandedDestinationFilterRulesActions = ({ stream }: Props) => (
  <IfPermitted permissions={[`streams:edit:${stream.id}`]}>
    <LinkContainer to={`${Routes.stream_view(stream.id)}?segment=destinations`}>
      <Button bsStyle="link" bsSize="xsmall" disabled={stream.is_default || !stream.is_editable}>
        Manage Filter Rules
      </Button>
    </LinkContainer>
  </IfPermitted>
);

export default ExpandedDestinationFilterRulesActions;
