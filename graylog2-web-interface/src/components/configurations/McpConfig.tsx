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
import { useEffect, useState } from 'react';

import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { getConfig } from 'components/configurations/helpers';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { BootstrapModalForm, Button, Input } from 'components/bootstrap';
// import { BootstrapModalForm, Button, Input, Table } from 'components/bootstrap';
import { IfPermitted, Select } from 'components/common';
import Spinner from 'components/common/Spinner';
import 'moment-duration-format';
import { DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';
import BetaBadge from 'components/common/BetaBadge';

type McpConfigState = {
  enable_remote_access: boolean;
  use_structured_output: boolean;
};

const McpConfig = () => {
  const [showConfigModal, setShowConfigModal] = useState<boolean>(false);
  const [modalConfig, setModalConfig] = useState<McpConfigState | undefined>(undefined);
  const [viewConfig, setViewConfig] = useState<McpConfigState | undefined>(undefined);
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);

  useEffect(() => {
    ConfigurationsActions.list(ConfigurationType.MCP_CONFIG).then(() => {
      const config = getConfig(ConfigurationType.MCP_CONFIG, configuration);
      setViewConfig(config);
      setModalConfig(config);
    });
  }, [configuration]);

  const openModal = () => {
    setShowConfigModal(true);
  };

  const onModalClickEnableRemoteAccess = () => {
    setModalConfig({ ...modalConfig, enable_remote_access: !modalConfig.enable_remote_access });
  };

  const onModalSetOutputFormat = (outputValue: string) => {
    setModalConfig({ ...modalConfig, use_structured_output: outputValue === "json" });
  };

  const onModalCancel = () => {
    setShowConfigModal(false);
    setModalConfig(viewConfig);
  };

  const onModalSave = () => {
    ConfigurationsActions.update(ConfigurationType.MCP_CONFIG, { ...modalConfig }).then(() => {
      setShowConfigModal(false);
    });
  };

  const outputFormatOptions = [
    { value: "string", label: "(String) Markdown" },
    { value: "json", label: "(JSON) Structured Content" },
  ];

  // const tools = [
  //   {name: "list_foo", ouputFormat: null, isEnabled: true},
  //   {name: "list_bar", ouputFormat: null, isEnabled: false},
  //   {name: "get_baz", ouputFormat: "(Overridden) JSON", isEnabled: true},
  // ];

  if (!viewConfig) {
    return <Spinner />;
  }

  return (
    <div>
      <h2>
        MCP Server Configuration <BetaBadge />
      </h2>
      <br/>
      <p>
        Activate MCP (Model Context Protocol) to enable LLM-powered communication and automation with your cluster.
      </p>
      <p>
        See the{' '}
        <DocumentationLink text="MCP connection documentation" page={DocsHelper.PAGES.MCP_SERVER} displayIcon={false} />{' '}
        for client setup instructions.
      </p>
      <hr/>
      <dl className="deflist">
        <dt>Remote MCP access</dt>
        <dd>{viewConfig.enable_remote_access ? 'Enabled' : 'Disabled'}</dd>
        <br/>
        <dt>Tool output format</dt>
        <dd>{viewConfig.use_structured_output ? 'JSON Structured Content' : 'Markdown'}</dd>
      </dl>

      {/*<br/>*/}
      {/*<h2>Available Tools</h2>*/}
      {/*<br/>*/}
      {/*<p>The following MCP Tools are available for use by MCP clients. Execution is restricted to <b>enabled</b> tools only.</p>*/}

      {/*<Table striped bordered condensed className="top-margin">*/}
      {/*  <thead>*/}
      {/*  <tr>*/}
      {/*    <th>Name</th>*/}
      {/*    /!*<th>Input Params</th>*!/*/}
      {/*    <th>Tool Output Format</th>*/}
      {/*    <th>Status</th>*/}
      {/*  </tr>*/}
      {/*  </thead>*/}
      {/*  <tbody>*/}
      {/*  {tools.map((tool) => (*/}
      {/*    <tr key={tool.name}>*/}
      {/*      <td>{tool.name}</td>*/}
      {/*      /!*<td>not supported yet</td>*!/*/}
      {/*      <td>{tool.ouputFormat ?? (modalConfig.use_structured_output ? "JSON" : "Markdown")}</td>*/}
      {/*      <td>{tool.isEnabled ? "🟢 enabled" : "🔴 disabled"}</td>*/}
      {/*    </tr>*/}
      {/*  ))}*/}
      {/*  </tbody>*/}
      {/*</Table>*/}
      {/*<br/>*/}

      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>
          Edit configuration
        </Button>
      </IfPermitted>

      {showConfigModal && modalConfig && (
        <BootstrapModalForm
          show
          bsSize="large"
          title="Update MCP Server Configuration"
          onSubmitForm={onModalSave}
          onCancel={onModalCancel}
          submitButtonText="Update configuration">
          <fieldset>
            <Input
              id="enable-remote-access-checkbox"
              type="checkbox"
              label="Enable remote MCP access"
              name="enabled"
              checked={modalConfig.enable_remote_access}
              onChange={onModalClickEnableRemoteAccess}
            />
            <Input
              id="output-format-input"
              label="Preferred tool output format"
              name="output-format"
            >
              <Select
                id="output-format-dropdown"
                disabled={!modalConfig.enable_remote_access}
                options={outputFormatOptions}
                value={outputFormatOptions.at(!modalConfig.use_structured_output ? 0 : 1).label}
                onChange={onModalSetOutputFormat}
              />
            </Input>
          </fieldset>
        </BootstrapModalForm>
      )}
    </div>
  );
};

export default McpConfig;
