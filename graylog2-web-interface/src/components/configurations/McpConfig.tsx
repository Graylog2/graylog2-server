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
import { BootstrapModalForm, Button, Input, Table, Badge } from 'components/bootstrap';
import { IfPermitted, Select, EntityDataTable} from 'components/common';
import Spinner from 'components/common/Spinner';
import 'moment-duration-format';
import { DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';
import BetaBadge from 'components/common/BetaBadge';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import type {EntityBase} from 'components/common/EntityDataTable/types';
import type {Sort} from 'stores/PaginationTypes';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';

type McpConfigState = {
  enable_remote_access: boolean;
  use_simple_output: boolean;
  enable_output_schema: boolean;
};

enum McpToolBehavior {
  ReadOnly    = 1 << 0,
  Destructive = 1 << 1,
  Idempotent  = 1 << 2,
  OpenWorld   = 1 << 3
}

type McpToolState = {
  name: string;  // read-only
  category: string;  // read-only
  behavior: McpToolBehavior;  // read-only
  format_overridden: boolean;  // read-only
  enabled: boolean;
  output_format: string;
  description: string;
}

type McpTools = Record<string, McpToolState>;

type McpToolEntity = EntityBase & {
  name: string;
  package: string;
  behavior: McpToolBehavior;
  format: string;
  status: string;
  description: string;
}

type toolTableView = {
  entities: McpToolEntity[];
  columns: (keyof McpToolEntity)[];
  sort: Sort;
}

const TABLE_COLUMNS: (keyof McpToolEntity)[] = ['name', 'package', 'behavior', 'format', 'status'];

const fetchTools = async () => {
  const url = '/mcp/api/tools';
  const { tools } = await fetch<{ tools: McpToolState[] }>('GET', qualifyUrl(url));

  return tools;
};

const patchTools = async (states: McpTools) => {
  const url = `/mcp/api/tools`;
  const payload: { name: string; enabled: boolean; output_format: string }[] = Object.entries(states).map(
    ([name, { enabled, output_format }]) => ({ name, enabled, output_format })
  );

  return fetch<{ errors?: string[] }>('POST', qualifyUrl(url), payload);
};

const expandedTool = (toolEntity: McpToolEntity) => (
  <div>
    <pre>{'> ' + toolEntity.name + "\n\n" + toolEntity.description}</pre>
  </div>
);

const ToolNameCell = ({ entity }: { entity: McpToolEntity }) => {
  const { toggleSection } = useExpandedSections();

  return (
    <Button
      onClick={() => toggleSection(entity.id, 'tool')}
      bsStyle='default'
      bsSize='xs'
    >
      {entity.name}
    </Button>
  );
};

const renderBehaviorBadges = (behavior: McpToolBehavior) : React.ReactNode => {
  const badges = Object.entries(McpToolBehavior)
    .filter(([, b]) => typeof b === 'number')
    .filter(([, b]) => (behavior & (b as number)) !== 0)
    .map(([name, b]) => {
      const val = b as McpToolBehavior;

      return <Badge
        key={val}
        bsStyle={val & (McpToolBehavior.Destructive | McpToolBehavior.OpenWorld) ? 'warning' : 'info'}
      >
        {name}
      </Badge>;
    });

  return <div>{badges.length != 0 ? badges : <i>Unknown</i>}</div>;
}

const McpConfig = () => {
  const [showConfigModal, setShowConfigModal] = useState<boolean>(false);
  const [modalConfig, setModalConfig] = useState<McpConfigState | undefined>(undefined);
  const [viewConfig, setViewConfig] = useState<McpConfigState | undefined>(undefined);
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);
  const [toolsView, setToolsView] = useState<toolTableView>({entities: [], columns: TABLE_COLUMNS, sort: {attributeId: 'package', direction: 'asc'}});
  const [modalTools, setModalTools] = useState<McpTools>({});

  const toRecordMap = (toolArray: McpToolState[]) => Object.fromEntries(
    toolArray.map(
      (tool) =>
        [tool.name, { ...tool, output_format: tool.format_overridden ? tool.output_format : "default" }]
    )
  ) as McpTools;

  const toEntity = (toolArray: McpToolState[]) => toolArray.map(
    (tool): McpToolEntity => ({
      id: tool.name,
      name: tool.name,
      package: tool.category,
      behavior: tool.behavior,
      format: tool.output_format + (tool.format_overridden ? '*' : ''),
      status: tool.enabled && '🟢 enabled' || '🔴 disabled',
      description: tool.description
    })
  );

  function getSorter(sort: Sort) {
    return (a: McpToolEntity, b: McpToolEntity): number => {
      let valueA: string = a[sort.attributeId];
      let valueB: string = b[sort.attributeId];

      if (sort.attributeId === 'package') {
        valueA = (valueA === 'core' ? '_' : '') + valueA;
        valueB = (valueB === 'core' ? '_' : '') + valueB;
      }

      if (valueA < valueB) return sort.direction === 'asc' ? -1 : 1;
      if (valueA > valueB) return sort.direction === 'asc' ? 1 : -1;

      return 0;
    };
  }

  useEffect(() => {
    ConfigurationsActions.list(ConfigurationType.MCP_CONFIG).then(() => {
      const config = getConfig(ConfigurationType.MCP_CONFIG, configuration);
      setViewConfig(config);
      setModalConfig(config);
    });
    fetchTools().then((data) => {
      setModalTools(toRecordMap(data));
      setToolsView((prev): toolTableView => ({...prev, entities: toEntity(data).toSorted(getSorter(prev.sort))}));
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
      use_simple_output: !is_structured,
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
        if (modalConfig.enable_remote_access) {
          patchTools(modalTools).then(fetchTools).then((data) => {
            setModalTools(toRecordMap(data))
            setToolsView((prev): toolTableView => ({...prev, entities: toEntity(data).toSorted(getSorter(prev.sort))}))
          });
        }
      })
      .finally(() => {
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
        <dd>{viewConfig.use_simple_output ? 'Markdown' : 'JSON Structured Content'}</dd>
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

      <EntityDataTable
        visibleColumns={toolsView.columns}
        columnsOrder={TABLE_COLUMNS}
        entities={toolsView.entities}
        activeSort={toolsView.sort}
        columnDefinitions={[
          { id: 'name', title: 'Name', sortable: true },
          { id: 'package', title: 'Package', sortable: true },
          { id: 'behavior', title: 'Operational Behavior', sortable: true },
          { id: 'format', title: 'Output Format', sortable: true },
          { id: 'status', title: 'Status', sortable: true },
        ]}
        entityAttributesAreCamelCase={false}
        onColumnsChange={(newCols: (keyof McpToolEntity)[]) =>
          setToolsView((prev) => ({
            ...prev,
            columns: newCols,
          }))
        }
        onSortChange={(newSort) =>
          setToolsView((prev) => ({
            ...prev,
            entities: prev.entities.toSorted(getSorter(newSort)),
            sort: newSort,
          }))
        }
        columnRenderers={{
          attributes: {
            name: {
              renderCell: (_value: string, entity: McpToolEntity) => <ToolNameCell entity={entity} />,
              staticWidth: 300,
            },
            behavior: {
              renderCell: renderBehaviorBadges,
            },
          },
        }}
        // entityActions={renderToolEntityActions}
        expandedSectionsRenderer={{
          tool: {
            title: 'Tool Details',
            content: expandedTool,
            disableHeader: true
          }
        }}
      />

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
              disabled={!modalConfig.enable_remote_access || modalConfig.use_simple_output}
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
                value={outputFormatOptions.at(modalConfig.use_simple_output ? 0 : 1).label}
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
