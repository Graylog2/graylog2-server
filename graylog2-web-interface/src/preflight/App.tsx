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
import {AppShell, Space} from '@mantine/core';

import Section from 'preflight/common/Section';
import Button from 'preflight/common/Button';
import Navigation from 'preflight/navigation/Navigation';
import DataNodesOverview from 'preflight/DataNodesOverview';
import CAConfiguration from 'preflight/CAConfiguration';

const App = () => (
  <AppShell padding="md" header={<Navigation />}>
    <Section title="Welcome!">
      <p>
        It looks like you are starting Graylog for the first time.
        Through this wizard, you can configure and secure your data nodes.
      </p>
      <Button size="xs">Continue</Button>
    </Section>

    <Section title="Data Node Certificate Authority Configuration">
      <DataNodesOverview />
      <Space h="md" />
      <CAConfiguration />
    </Section>
  </AppShell>
);
export default App;
