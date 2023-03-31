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
import {Title, Grid, List} from '@mantine/core';

const CAConfigurationWizard = () => (
  <Grid>
    <Grid.Col span={6}>
      <Title order={3}>Configure Certificate Authority</Title>
      <p>
        In this first step you can either upload or create a new certificate authority.
        Using it we can provision your data nodes with certificates easily.
      </p>

    </Grid.Col>
    <Grid.Col span={6}>
      <List>
        <List.Item>
          1. Configure a certificate authority
        </List.Item>
        <List.Item>
          2. Provision certificates for your data nodes.
        </List.Item>
        <List.Item>
          3. All data nodes are secured and reachable.
        </List.Item>
      </List>
    </Grid.Col>
  </Grid>
);

export default CAConfigurationWizard;
