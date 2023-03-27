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
import React, { useState, useEffect } from 'react';
import { Form, Formik } from 'formik';
import capitalize from 'lodash/capitalize';
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';
import 'components/indices/rotation';
import 'components/indices/retention';
import { PluginStore } from 'graylog-web-plugin/plugin';

import type { MaintenanceOptions, RotationStrategyConfig, RetentionStrategyConfig } from 'components/indices/Types';
import { IndicesConfigurationActions } from 'stores/indices/IndicesConfigurationStore';
import IndexMaintenanceStrategiesConfiguration from 'components/indices/IndexMaintenanceStrategiesConfiguration';
import { Button, Col, Modal, Row } from 'components/bootstrap';
import { IfPermitted, TimeUnitInput, Spinner } from 'components/common';
import IndexMaintenanceStrategiesSummary from 'components/indices/IndexMaintenanceStrategiesSummary';
import { TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY } from 'stores/indices/IndicesStore';

import FormikInput from '../common/FormikInput';

export type IndexConfig = {
  index_prefix: string,
  index_analyzer: string,
  shards: number,
  replicas: number,
  index_optimization_max_num_segments: number,
  index_optimization_disabled: boolean,
  field_type_refresh_interval: number,
  field_type_refresh_interval_unit: 'seconds' | 'minutes',
  rotation_strategy_config: RotationStrategyConfig,
  rotation_strategy_class: string,
  retention_strategy_config: RetentionStrategyConfig,
  retention_strategy_class: string,
}

const TIME_UNITS = ['SECONDS', 'MINUTES'];

type Props = {
  initialConfig: IndexConfig,
  updateConfig: (arg: IndexConfig) => object,
};

const StyledDefList = styled.dl.attrs({
  className: 'deflist',
})(({ theme }: { theme: DefaultTheme }) => css`
  &&.deflist {
    dd {
      padding-left: ${theme.spacings.md};
      margin-left: 200px;
    }
  }
`);

const IndexSetsDefaultsConfig = ({ initialConfig, updateConfig }: Props) => {
  const [showModal, setShowModal] = useState<boolean>(false);
  const [rotationStrategies, setRotationStrategies] = useState<MaintenanceOptions>();
  const [retentionStrategies, setRetentionStrategies] = useState<MaintenanceOptions>();
  const [currentConfig, setCurrentConfig] = useState<IndexConfig>(initialConfig);
  const handleSaveConfig = async (configToSave: IndexConfig) => updateConfig(configToSave);

  useEffect(() => {
    IndicesConfigurationActions.loadRotationStrategies().then((loadedRotationStrategies) => {
      setRotationStrategies(loadedRotationStrategies);
    });

    IndicesConfigurationActions.loadRetentionStrategies().then((loadedRetentionStrategies) => {
      setRetentionStrategies(loadedRetentionStrategies);
    });
  }, []);

  const saveConfig = (values, { setSubmitting }) => {
    const defaultIndexValues = { ...values };

    if (defaultIndexValues.rotation_strategy_class === TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY) {
      defaultIndexValues.rotation_strategy_config = defaultIndexValues?.rotation_strategy;
    }

    delete defaultIndexValues?.rotation_strategy;
    delete defaultIndexValues?.retention_strategy;

    handleSaveConfig(defaultIndexValues)
      .then((config) => {
        setCurrentConfig(config as IndexConfig);
        setShowModal(false);
      })
      .catch(() => {
        setSubmitting(false);
      });
  };

  const resetConfig = () => {
    setShowModal(false);
  };

  if (!rotationStrategies || !rotationStrategies || !currentConfig) {
    return <Spinner />;
  }

  const rotationConfig = {
    config: currentConfig.rotation_strategy_config,
    strategy: currentConfig.rotation_strategy_class,
  };
  const retentionConfig = {
    config: currentConfig.retention_strategy_config,
    strategy: currentConfig.retention_strategy_class,
  };

  const getRotationConfigState = (strategy: string, data: string) => {
    return {
      rotation_strategy_config: data,
      rotation_strategy_class: strategy,
    };
  };

  const getRetentionConfigState = (strategy: string, data: string) => {
    return {
      retention_strategy_class: strategy,
      retention_strategy_config: data,
    };
  };

  const modalTitle = 'Configure Index Set Defaults';

  return (
    <div>
      <h2>Index Defaults</h2>
      <p>Defaults for newly created index sets.</p>
      <StyledDefList>
        <dt>Index analyzer:</dt>
        <dd>{currentConfig.index_analyzer}</dd>
        <dt>Shards per Index:</dt>
        <dd>{currentConfig.shards}</dd>
        <dt>Replicas per Index:</dt>
        <dd>{currentConfig.replicas}</dd>
        <dt>Index optimization disabled:</dt>
        <dd>{currentConfig.index_optimization_disabled ? 'Yes' : 'No'}</dd>
        <dt>Max. Number of Segments:</dt>
        <dd>{currentConfig.index_optimization_max_num_segments}</dd>
        <dt>Field type refresh interval:</dt>
        <dd>{currentConfig.field_type_refresh_interval} {capitalize(currentConfig.field_type_refresh_interval_unit)}</dd>
        <br />
        <IndexMaintenanceStrategiesSummary config={rotationConfig}
                                           pluginExports={PluginStore.exports('indexRotationConfig')} />
        <IndexMaintenanceStrategiesSummary config={retentionConfig}
                                           pluginExports={PluginStore.exports('indexRetentionConfig')} />
      </StyledDefList>

      <p>
        <IfPermitted permissions="indices:changestate">
          <Button bsStyle="info"
                  bsSize="xs"
                  onClick={() => {
                    setShowModal(true);
                  }}>Edit configuration
          </Button>
        </IfPermitted>
      </p>

      <Modal show={showModal}
             onHide={resetConfig}
             aria-modal="true"
             aria-labelledby="dialog_label"
             data-app-section="configurations_index_defaults"
             data-event-element={modalTitle}>
        <Formik onSubmit={saveConfig} initialValues={currentConfig}>
          {({ values, setFieldValue, isSubmitting }) => {
            return (
              <Form>
                <Modal.Header closeButton>
                  <Modal.Title id="dialog_label">{modalTitle}</Modal.Title>
                </Modal.Header>

                <Modal.Body>
                  <div>
                    <Row>
                      <Col md={12}>
                        <FormikInput label="Index Analyzer"
                                     name="index_analyzer"
                                     id="index_analyzer" />
                        <FormikInput label="Shards per Index"
                                     name="shards"
                                     id="shards" />
                        <FormikInput label="Replicas"
                                     name="replicas"
                                     id="replicas" />
                        <FormikInput label="Index Optimization Disabled"
                                     type="checkbox"
                                     name="index_optimization_disabled"
                                     id="index_optimization_disabled" />
                        <FormikInput label="Max. Number of Segments"
                                     name="index_optimization_max_num_segments"
                                     id="index_optimization_max_num_segments" />
                        <TimeUnitInput label="Field type refresh interval"
                                       update={(value, unit) => {
                                         setFieldValue('field_type_refresh_interval', value);
                                         setFieldValue('field_type_refresh_interval_unit', unit);
                                       }}
                                       value={values.field_type_refresh_interval}
                                       unit={values.field_type_refresh_interval_unit}
                                       enabled
                                       hideCheckbox
                                       units={TIME_UNITS} />
                        <IndexMaintenanceStrategiesConfiguration title="Index Rotation Configuration"
                                                                 name="rotation"
                                                                 selectPlaceholder="Select rotation strategy"
                                                                 pluginExports={PluginStore.exports('indexRotationConfig')}
                                                                 strategies={rotationStrategies.strategies}
                                                                 activeConfig={rotationConfig}
                                                                 getState={getRotationConfigState} />

                        <IndexMaintenanceStrategiesConfiguration title="Index Retention Configuration"
                                                                 name="retention"
                                                                 selectPlaceholder="Select rotation strategy"
                                                                 pluginExports={PluginStore.exports('indexRetentionConfig')}
                                                                 strategies={retentionStrategies.strategies}
                                                                 activeConfig={retentionConfig}
                                                                 getState={getRetentionConfigState} />
                      </Col>
                    </Row>
                  </div>
                </Modal.Body>

                <Modal.Footer>
                  <Button type="button" bsStyle="link" onClick={resetConfig}>Close</Button>
                  <Button type="submit"
                          bsStyle="success"
                          disabled={isSubmitting}>{isSubmitting ? 'Saving' : 'Save'}
                  </Button>
                </Modal.Footer>
              </Form>
            );
          }}
        </Formik>
      </Modal>
    </div>
  );
};

export default IndexSetsDefaultsConfig;
