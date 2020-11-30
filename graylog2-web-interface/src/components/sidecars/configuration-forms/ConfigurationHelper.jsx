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
import PropTypes from 'prop-types';
import React from 'react';

import { Col, Panel, Row, Tab, Tabs } from 'components/graylog';

import TemplatesHelper from './TemplatesHelper';
import ConfigurationVariablesHelper from './ConfigurationVariablesHelper';
import ConfigurationHelperStyle from './ConfigurationHelper.css';

class ConfigurationHelper extends React.Component {
  static propTypes = {
    onVariableRename: PropTypes.func.isRequired,
  };

  _getId = (idName, index) => {
    const idIndex = index !== undefined ? `. ${index}` : '';

    return idName + idIndex;
  };

  render() {
    const { onVariableRename } = this.props;

    return (
      /* eslint-disable no-template-curly-in-string */
      <Panel header="Collector Configuration Reference">

        <Row className="row-sm">
          <Col md={12}>
            <Tabs id="configurationsHelper" defaultActiveKey={1} animation={false}>
              <Tab eventKey={1} title="Runtime Variables">
                <p className={ConfigurationHelperStyle.marginQuickReferenceText}>
                  These variables will be filled with the runtime information from each Sidecar
                </p>
                <TemplatesHelper />
              </Tab>
              <Tab eventKey={2} title="Variables">
                <p className={ConfigurationHelperStyle.marginQuickReferenceText}>
                  Use variables to share text snippets across multiple configurations.
                  <br />
                  If your configuration format needs to use literals like <code>$&#123;foo&#125;</code>,
                  which shall not act as a variable, you will have to write it as
                  <code>$&#123;&apos;$&apos;&#125;&#123;foo&#125;</code>.
                </p>
                <ConfigurationVariablesHelper onVariableRename={onVariableRename} />
              </Tab>
              <Tab eventKey={3} title="Reference">
                <Row className="row-sm">
                  <Col md={12}>
                    <p className={ConfigurationHelperStyle.marginQuickReferenceText}>
                      We provide collector configuration templates to get you started.<br />
                      For further information please refer to the official documentation of your collector.
                    </p>
                    <ul className={ConfigurationHelperStyle.ulStyle}>
                      <li><a href="https://www.elastic.co/guide/en/beats/filebeat/current/index.html" target="_blank" rel="noopener noreferrer">Filebeat Reference</a> </li>
                      <li><a href="https://www.elastic.co/guide/en/beats/winlogbeat/current/index.html" target="_blank" rel="noopener noreferrer">Winlogbeat Reference</a> </li>
                      <li><a href="https://nxlog.co/docs/nxlog-ce/nxlog-reference-manual.html" target="_blank" rel="noopener noreferrer">NXLog Reference Manual</a> </li>
                    </ul>
                  </Col>
                </Row>
              </Tab>
            </Tabs>
          </Col>
        </Row>
      </Panel>
    );
    /* eslint-enable no-template-curly-in-string */
  }
}

export default ConfigurationHelper;
