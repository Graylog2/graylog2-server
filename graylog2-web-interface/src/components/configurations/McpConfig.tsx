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
import { BootstrapModalForm, Button, Input, Table } from 'components/bootstrap';
import { IfPermitted, Select } from 'components/common';
// import { IfPermitted, Select, EntityDataTable} from 'components/common';
import Spinner from 'components/common/Spinner';
import 'moment-duration-format';
import { DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';
import BetaBadge from 'components/common/BetaBadge';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

type McpConfigState = {
  enable_remote_access: boolean;
  use_structured_output: boolean;
  enable_output_schema: boolean;
};

type McpToolState = {
  name: string;  // read-only
  category: string;  // read-only
  read_only: boolean;  // read-only
  format_overridden: boolean;  // read-only
  enabled: boolean;
  output_format: string;
}

type McpTools = Record<string, McpToolState>;

const fetchTools = () => {
  const url = "/mcp/api/tools";

  return fetch<{ tools: McpToolState[] }>('GET', qualifyUrl(url)).then(
    ({ tools }) => tools
  );
};

const patchTools = (states: McpTools) => {
  const url = `/mcp/api/tools`;

  const payload: { name: string; enabled: boolean; output_format: string }[] = Object.entries(states).map(
    ([name, { enabled, output_format }]) => ({ name, enabled, output_format })
  );

  return fetch<{ errors?: string[] }>('POST', qualifyUrl(url), payload);
};

const McpConfig = () => {
  const [showConfigModal, setShowConfigModal] = useState<boolean>(false);
  const [modalConfig, setModalConfig] = useState<McpConfigState | undefined>(undefined);
  const [viewConfig, setViewConfig] = useState<McpConfigState | undefined>(undefined);
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);
  const [tools, setTools] = useState<McpToolState[]>([]);
  const [modalTools, setModalTools] = useState<McpTools>({});

  useEffect(() => {
    ConfigurationsActions.list(ConfigurationType.MCP_CONFIG).then(() => {
      const config = getConfig(ConfigurationType.MCP_CONFIG, configuration);
      setViewConfig(config);
      setModalConfig(config);
    });
  }, [configuration]);

  const toRecordMap = (toolArray: McpToolState[]) => Object.fromEntries(
    toolArray.map(
      (tool) =>
        [tool.name, { ...tool, output_format: tool.format_overridden ? tool.output_format : "default" }]
    )
  ) as McpTools;

  // const toEntity = (toolArray: McpToolState[]) => toolArray.map(
  //   (tool) => ({
  //     id: tool.name,
  //     name: tool.name,
  //     package: tool.category,
  //     access: tool.read_only ? 'Read Only' : 'Read/Write',
  //     output_format: tool.output_format + (tool.format_overridden ? '*' : ''),
  //     status: tool.enabled ? '🟢 enabled' : '🔴 disabled',
  //   })
  // );

  useEffect(() => {
    fetchTools().then((data) => {
      setTools(data);
      setModalTools(toRecordMap(data))
    });
  }, [configuration]);

  const openModal = () => {
    setShowConfigModal(true);
  };

  const onModalClickEnableRemoteAccess = () => {
    setModalConfig({ ...modalConfig, enable_remote_access: !modalConfig.enable_remote_access });
  };

  const onModalClickEnableOutputSchema = () => {
    setModalConfig({ ...modalConfig, enable_output_schema: !modalConfig.enable_output_schema });
  };

  const onModalSetOutputFormat = (outputValue: string) => {
    const is_structured: boolean = outputValue === "json";
    setModalConfig({
      ...modalConfig,
      use_structured_output: is_structured,
      enable_output_schema: is_structured && modalConfig.enable_output_schema });
  };

  const onModalClickEnableTool = (name: string) => {
    setModalTools(prev => ({...prev, [name]: { ...prev[name], enabled: !prev[name].enabled } }));
  }

  const onModalSetOutputFormatOverride = (name: string, format: string) => {
    setModalTools(prev => ({...prev, [name]: { ...prev[name], output_format: format } }));
  }

  const onModalCancel = () => {
    setShowConfigModal(false);
    setModalConfig(viewConfig);
  };

  const onModalSave = () => {
    ConfigurationsActions.update(ConfigurationType.MCP_CONFIG, { ...modalConfig })
      .then(() => {
        patchTools(modalTools).then(fetchTools).then((data) => {
          setTools(data);
          setModalTools(toRecordMap(data))
        });
      })
      .then(() => {
        setShowConfigModal(false);
      });
  };

  const outputFormatOptions = [
    { value: "string", label: "(String) Markdown" },
    { value: "json", label: "(JSON) Structured Content" },
  ];

  if (!viewConfig) {
    return <Spinner />;
  }

  return (
    <div>
      <h2>
        MCP Server Configuration <BetaBadge />
      </h2>
      <br />
      <p>Activate MCP (Model Context Protocol) to enable LLM-powered communication and automation with your cluster.</p>
      <p>
        See the{' '}
        <DocumentationLink text="MCP connection documentation" page={DocsHelper.PAGES.MCP_SERVER} displayIcon={false} />{' '}
        for client setup instructions.
      </p>
      <hr />
      <dl className="deflist">
        <dt>Remote MCP access</dt>
        <dd>{viewConfig.enable_remote_access ? 'Enabled' : 'Disabled'}</dd>
        <br />
        <dt>Default output format</dt>
        <dd>{viewConfig.use_structured_output ? 'JSON Structured Content' : 'Markdown'}</dd>
        <br />
        <dt>Output schema</dt>
        <dd>{viewConfig.enable_output_schema ? 'Enabled' : 'Disabled'}</dd>
      </dl>

      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>
          Edit configuration
        </Button>
      </IfPermitted>

      <br />
      <br />
      <br />
      <h2>Available Tools</h2>
      <br />
      <p>
        The following MCP Tools are available for use by MCP clients. Only <b>enabled</b> tools are allowed to run in
        the server.
      </p>

      {/*<EntityDataTable*/}
      {/*  visibleColumns={['name', 'package', 'access', 'output_format', 'status']}*/}
      {/*  entities={toEntity(tools)}*/}
      {/*  columnDefinitions={[*/}
      {/*    { id: 'name', title: 'Name' },*/}
      {/*    { id: 'package', title: 'Package' },*/}
      {/*    { id: 'access', title: 'Access'},*/}
      {/*    { id: 'output_format', title: 'Output Format'},*/}
      {/*    { id: 'status', title: 'Status'}*/}
      {/*  ]}*/}
      {/*  entityAttributesAreCamelCase={false}*/}
      {/*  onColumnsChange={() => {}}*/}
      {/*  onSortChange={() => {}}*/}
      {/*/>*/}

      <Table striped bordered condensed className="top-margin">
        <thead>
          <tr>
            <th>Name</th>
            <th>Package</th>
            <th>Access</th>
            <th>Output Format</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {tools.map((tool) => (
            <tr key={tool.name}>
              <td>
                &nbsp;&nbsp;<i>{tool.name}</i>
              </td>
              <td>{tool.category}</td>
              <td>{tool.read_only ? 'Read Only' : <b>Read/Write</b>}</td>
              <td>{tool.output_format + (tool.format_overridden ? '*' : '')}</td>
              <td>{tool.enabled ? '🟢 enabled' : '🔴 disabled'}</td>
            </tr>
          ))}
        </tbody>
      </Table>
      <br />

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
              id="enable-output-schema-checkbox"
              disabled={!modalConfig.enable_remote_access || !modalConfig.use_structured_output}
              type="checkbox"
              label="Enable Output Schema generation"
              name="output-schema-enabled"
              checked={modalConfig.enable_output_schema}
              onChange={onModalClickEnableOutputSchema}
            />
            <Input id="output-format-input" label="Preferred tool output format" name="output-format">
              <Select
                id="output-format-dropdown"
                disabled={!modalConfig.enable_remote_access}
                options={outputFormatOptions}
                value={outputFormatOptions.at(!modalConfig.use_structured_output ? 0 : 1).label}
                onChange={onModalSetOutputFormat}
              />
            </Input>
          </fieldset>
          <br />
          <h3>MCP Tools</h3>
          <br />
          <p>
            Click the checkbox to enable/disable the corresponding MCP tool. Choose an output format to override the
            default tool output format.
          </p>
          <Table striped bordered condensed className="top-margin">
            <thead>
              <tr>
                <th>Tool</th>
                <th>Output format override</th>
                <th>Enabled</th>
              </tr>
            </thead>
            <tbody>
              {Object.entries(modalTools).map(([name, tool]) => (
                <tr key={name}>
                  <td>{name}</td>
                  <td>
                    {
                      <Select
                        id={name + '-dropdown'}
                        disabled={!modalConfig.enable_remote_access}
                        options={outputFormatOptions}
                        value={tool.format_overridden ? tool.output_format : ''}
                        onChange={(value) => {
                          onModalSetOutputFormatOverride(name, value);
                        }}
                      />
                    }
                  </td>
                  {/* eslint-disable-next-line jsx-a11y/control-has-associated-label */}
                  <td>
                    <input
                      type="checkbox"
                      disabled={!modalConfig.enable_remote_access}
                      checked={tool.enabled}
                      onChange={() => {
                        onModalClickEnableTool(name);
                      }}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </BootstrapModalForm>
      )}
    </div>
  );
};

export default McpConfig;
