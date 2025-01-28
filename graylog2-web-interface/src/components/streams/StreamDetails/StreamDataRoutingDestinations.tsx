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
import { PluginStore } from 'graylog-web-plugin/plugin';
import styled, { css } from 'styled-components';

import type { Stream } from 'stores/streams/StreamsStore';
import DestinationOutputs from 'components/streams/StreamDetails/routing-destination/DestinationOutputs';
import DestinationIndexSetSection from 'components/streams/StreamDetails/routing-destination/DestinationIndexSetSection';
import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';

import DestinationPermissionAlert from './DestinationPermissionAlert';

type Props = {
  stream: Stream;
};

const Container = styled.div(({ theme }) => css`
  > div {
    margin-bottom: ${theme.spacings.sm};
  }
`);

const StreamDataRoutingDestinations = ({ stream }: Props) => {
  const currentUser = useCurrentUser();
  const StreamDataWarehouseComponent = PluginStore.exports('dataWarehouse')?.[0]?.StreamDataWarehouse;

  const destinationIndexset = isPermitted(currentUser.permissions, ['indexsets:read']) ? <DestinationIndexSetSection stream={stream} /> : <DestinationPermissionAlert sectionName="Index Set" />;
  const destinationOutput = isPermitted(currentUser.permissions, ['output:read']) ? <DestinationOutputs stream={stream} /> : <DestinationPermissionAlert sectionName="Outputs" />;

  return (
    <Container>
      {destinationIndexset}
      {StreamDataWarehouseComponent && <StreamDataWarehouseComponent permissions={currentUser.permissions} />}
      {destinationOutput}
    </Container>
  );
};

export default StreamDataRoutingDestinations;
