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
