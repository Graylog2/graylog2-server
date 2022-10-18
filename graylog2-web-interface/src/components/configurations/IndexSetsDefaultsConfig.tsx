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
import React, {useState, useEffect} from 'react';

import {Form, Formik} from 'formik';

import {IfPermitted, TimeUnitInput, Spinner} from 'components/common';
import {Button, Col, Modal, Row} from 'components/bootstrap';
import FormikInput from '../common/FormikInput';
import lodash from 'lodash';
import styled, {DefaultTheme, css} from 'styled-components';

import 'components/indices/rotation';
import 'components/indices/retention';
import {RotationStrategy} from 'components/indices/Types';
import {IndicesConfigurationActions} from 'stores/indices/IndicesConfigurationStore';
import IndexMaintenanceStrategiesConfiguration from 'components/indices/IndexMaintenanceStrategiesConfiguration';
import {PluginStore} from 'graylog-web-plugin/plugin';

{/* Matches backend index defaults persistence configuration. */
}
export type IndexConfig = {
  index_prefix: string,
  index_analyzer: string,
  shards: number,
  replicas: number,
  index_optimization_max_num_segments: number,
  index_optimization_disabled: boolean,
  field_type_refresh_interval: number,
  field_type_refresh_interval_unit: 'seconds' | 'minutes',
  rotation_strategy_config: string,
  rotation_strategy_class: string,
}

export type LegacyRetentionConfig = {
  config: string,
  strategy: string,
}

const TIME_UNITS = ['SECONDS', 'MINUTES'];

type Props = {
  config: IndexConfig,
  updateConfig: (arg: IndexConfig) => void,
  rotationStrategies?: Array<RotationStrategy> | null | undefined,
};

const StyledDefList = styled.dl.attrs({
  className: 'deflist',
})(({theme}: { theme: DefaultTheme }) => css`
  &&.deflist {
    dd {
      padding-left: ${theme.spacings.md};
      margin-left: 200px;
    }
  }
`);

const getRotationConfigState = (strategy: string, data: string) => {
  return {rotation_strategy_class: strategy, rotation_strategy_config: data};
};

const IndexSetsDefaultsConfig = ({config, updateConfig}: Props) => {
  const [showModal, setShowModal] = useState<boolean>(false);
  const [rotationStrategies, setRotationStrategies] = useState<unknown>(false);
  const handleSaveConfig = async (config: IndexConfig) => updateConfig(config);
  const saveConfig = (values) => {
    handleSaveConfig(values).then(() => {
      setShowModal(false);
    });
  };

  const resetConfig = () => {
    setShowModal(false);
  };

  // The component expects a different structure - legacy
  const activeConfig: LegacyRetentionConfig = {
    config: config.rotation_strategy_config,
    strategy: config.rotation_strategy_class,
  };

  useEffect(() => {
    IndicesConfigurationActions.loadRotationStrategies().then((rotationStrategies) => {
      setRotationStrategies(rotationStrategies);
    });
  }, []);

  if (!rotationStrategies) {
    return <Spinner />;
  } else {
    return (
      <div>
        <h2>Index Defaults</h2>
        <p>Defaults for newly created index sets.</p>
        <StyledDefList>
          <dt>Index prefix:</dt>
          <dd>{!config.index_prefix ? '<none>' : config.index_prefix}</dd>
          <dt>Index analyzer:</dt>
          <dd>{config.index_analyzer}</dd>
          <dt>Shards per Index:</dt>
          <dd>{config.shards}</dd>
          <dt>Replicas per Index:</dt>
          <dd>{config.replicas}</dd>
          <dt>Max. Number of Segments:</dt>
          <dd>{config.index_optimization_max_num_segments}</dd>
          <dt>Index optimization disabled:</dt>
          <dd>{config.index_optimization_disabled ? 'Yes' : 'No'}</dd>
          <dt>Field type refresh interval:</dt>
          <dd>{config.field_type_refresh_interval} {lodash.capitalize(config.field_type_refresh_interval_unit)}</dd>
        </StyledDefList>

        <p>
          <IfPermitted permissions="indices:changestate">
            <Button
              bsStyle="info"
              bsSize="xs"
              onClick={() => {
                setShowModal(true);
              }}>Update</Button>
          </IfPermitted>
        </p>

        <Modal show={showModal} onHide={resetConfig} aria-modal="true" aria-labelledby="dialog_label">
          <Formik onSubmit={saveConfig} initialValues={config}>
            {({values, setFieldValue, isSubmitting}) => {
              return (
                <Form>
                  <Modal.Header closeButton>
                    <Modal.Title id="dialog_label">Configure Index Set Defaults</Modal.Title>
                  </Modal.Header>

                  <Modal.Body>
                    <div>
                      <Row>
                        <Col sm={12}>
                          {/* TODO: Descriptions and help text <InputDescription help={<>A relevant description</>} />*/}
                          {/* TODO: Analyzer validation? Dropdown?*/}
                          <FormikInput label="Index Prefix"
                                       name="index_prefix"
                                       id="index_prefix"
                                       help="The prefix." />
                          <FormikInput label="Index Analyzer"
                                       name="index_analyzer"
                                       id="index_analyzer" />
                          <FormikInput label="Shards per Index"
                                       name="shards"
                                       id="shards" />
                          <FormikInput label="Replicas"
                                       name="replicas"
                                       id="replicas" />
                          <FormikInput label="Max. Number of Segments"
                                       name="index_optimization_max_num_segments"
                                       id="index_optimization_max_num_segments" />
                          <FormikInput label="Index Optimization Disabled"
                                       type="checkbox"
                                       name="index_optimization_disabled"
                                       id="index_optimization_disabled" />
                          <TimeUnitInput label="Field type refresh interval"
                                         update={(value, unit) => {
                                           setFieldValue('field_type_refresh_interval', value);
                                           setFieldValue('field_type_refresh_interval_unit', unit);
                                         }}
                                         value={values.field_type_refresh_interval}
                                         unit={values.field_type_refresh_interval_unit}
                                         enabled={true}
                                         hideCheckbox
                                         units={TIME_UNITS} />

                          <IndexMaintenanceStrategiesConfiguration title="Index Rotation Configuration"
                                                                   key="rotation"
                                                                   description="Graylog uses multiple indices to store documents in. You can configure the strategy it uses to determine when to rotate the currently active write index."
                                                                   selectPlaceholder="Select rotation strategy"
                                                                   pluginExports={PluginStore.exports('indexRotationConfig')}
                                                                   strategies={rotationStrategies.strategies}
                                                                   activeConfig={activeConfig}
                                                                   getState={getRotationConfigState} />
                        </Col>
                      </Row>
                    </div>
                  </Modal.Body>

                  <Modal.Footer>
                    <Button type="button" bsStyle="link" onClick={resetConfig}>Close</Button>
                    <Button type="submit" bsStyle="success"
                            disabled={isSubmitting}>{isSubmitting ? 'Saving' : 'Save'}</Button>
                  </Modal.Footer>
                </Form>
              );
            }}
          </Formik>
        </Modal>
      </div>
    );
  }
};

export default IndexSetsDefaultsConfig;
