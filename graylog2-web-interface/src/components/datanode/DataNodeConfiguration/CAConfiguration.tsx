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

import { Tabs, Tab } from 'components/bootstrap';
import CACreateForm from 'components/datanode/DataNodeConfiguration/CACreateForm';
import CAUpload from 'components/datanode/DataNodeConfiguration/CAUpload';

const TAB_KEYS = ['create', 'upload'];
const CAConfiguration = () => (
  <>
    <h2>Configure Certificate Authority</h2>
    <p>
      In this first step you can either upload or create a new certificate authority.<br />
      Using it we can provision your data nodes with certificates easily.
    </p>
    <Tabs defaultActiveKey={TAB_KEYS[0]} id="ca-configurations">
      <Tab eventKey={TAB_KEYS[0]} title="Create new CA">
        <CACreateForm />
      </Tab>
      <Tab eventKey={TAB_KEYS[1]} title="Upload CA">
        <CAUpload />
      </Tab>
    </Tabs>
  </>
);
export default CAConfiguration;
