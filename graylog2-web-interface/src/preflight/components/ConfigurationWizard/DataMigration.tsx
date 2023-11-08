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

import { Title, Space, Button } from 'preflight/components/common';

type Props = {
  setShouldMigrateData: React.Dispatch<React.SetStateAction<boolean>>,
}

const DataMigration = ({ setShouldMigrateData } : Props) => (
  <div>
    <Title order={3}>Migrate data from existing node</Title>
    <p>Migrate your Data</p>
    <Space h="md" />
    <Button size="xs" onClick={() => setShouldMigrateData(false)}>Cancel</Button>
  </div>
);

export default DataMigration;
