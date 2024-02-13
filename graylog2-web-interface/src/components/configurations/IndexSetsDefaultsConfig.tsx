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
import styled, { css } from 'styled-components';
import 'components/indices/rotation';
import 'components/indices/retention';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { getConfig } from 'components/configurations/helpers';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import type { MaintenanceOptions, RotationStrategyConfig, RetentionStrategyConfig } from 'components/indices/Types';
import { IndicesConfigurationActions } from 'stores/indices/IndicesConfigurationStore';
import IndexMaintenanceStrategiesConfiguration from 'components/indices/IndexMaintenanceStrategiesConfiguration';
import { Button, Col, Modal, Row } from 'components/bootstrap';
import { IfPermitted, TimeUnitInput, Spinner } from 'components/common';
import IndexMaintenanceStrategiesSummary from 'components/indices/IndexMaintenanceStrategiesSummary';
import IndexRetentionProvider from 'components/indices/contexts/IndexRetentionProvider';
import { TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY } from 'stores/indices/IndicesStore';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { IndexSetsDefaultConfiguration } from 'stores/indices/IndexSetsStore';

import FormikInput from '../common/FormikInput';

const TIME_UNITS = ['SECONDS', 'MINUTES'];

const StyledDefList = styled.dl.attrs({ className: 'deflist' })(({ theme }) => css`
  &&.deflist {
    dd {
      padding-left: ${theme.spacings.md};
      margin-left: 200px;
    }
  }
`);

const IndexSetsDefaultsConfig = () => {
  const [showModal, setShowModal] = useState<boolean>(false);
  const [viewConfig, setViewConfig] = useState<IndexSetsDefaultConfiguration | undefined>(undefined);
  const [formConfig, setFormConfig] = useState<IndexSetsDefaultConfiguration | undefined>(undefined);
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);
  const [rotationStrategies, setRotationStrategies] = useState<MaintenanceOptions>();
  const [retentionStrategies, setRetentionStrategies] = useState<MaintenanceOptions>();

  const handleSaveConfig = async (configToSave: IndexSetsDefaultConfiguration) => (
    ConfigurationsActions.updateIndexSetDefaults(ConfigurationType.INDEX_SETS_DEFAULTS_CONFIG, configToSave)
  );

  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  useEffect(() => {
    ConfigurationsActions.list(ConfigurationType.INDEX_SETS_DEFAULTS_CONFIG).then(() => {
      IndicesConfigurationActions.loadRotationStrategies().then((loadedRotationStrategies) => {
        setRotationStrategies(loadedRotationStrategies);
      });

      IndicesConfigurationActions.loadRetentionStrategies().then((loadedRetentionStrategies) => {
        setRetentionStrategies(loadedRetentionStrategies);
      });

      const config = getConfig(ConfigurationType.INDEX_SETS_DEFAULTS_CONFIG, configuration);

      setViewConfig(config);
      setFormConfig(config);
    });
  }, [configuration]);

  const saveConfig = (values, { setSubmitting }) => {
    const defaultIndexValues = { ...values };

    if (defaultIndexValues.rotation_strategy_class === TIME_BASED_SIZE_OPTIMIZING_ROTATION_STRATEGY) {
      defaultIndexValues.rotation_strategy_config = defaultIndexValues?.rotation_strategy;
    }

    delete defaultIndexValues?.rotation_strategy;
    delete defaultIndexValues?.retention_strategy;

    sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.INDEX_SETS_UPDATED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'index-default',
      app_action_value: 'configuration-save',
    });

    handleSaveConfig(defaultIndexValues)
      .then(() => {
        setShowModal(false);
      })
      .catch(() => {
        setSubmitting(false);
      });
  };

  const resetConfig = () => {
    setShowModal(false);
    setFormConfig(viewConfig);
  };

  if (!rotationStrategies || !rotationStrategies || !viewConfig) {
    return <Spinner />;
  }

  const rotationConfig = (configToUse) => ({
    config: configToUse.rotation_strategy_config,
    strategy: configToUse.rotation_strategy_class,
  });

  const retentionConfig = (configToUse) => ({
    config: configToUse.retention_strategy_config,
    strategy: configToUse.retention_strategy_class,
  });

  const getRotationConfigState = (strategy: string, data: RotationStrategyConfig) => ({
    rotation_strategy_config: data,
    rotation_strategy_class: strategy,
  });

  const getRetentionConfigState = (strategy: string, data: RetentionStrategyConfig) => ({
    retention_strategy_class: strategy,
    retention_strategy_config: data,
  });

  const modalTitle = 'Configure Index Set Defaults';

  return (
    <IndexRetentionProvider>
      <div>
        <h2>Index Set Defaults Configuration</h2>
        <p>Defaults for newly created index sets.</p>
        {!viewConfig || !formConfig ? <Spinner /> : (
          <>
            <StyledDefList>
              <dt>Index analyzer:</dt>
              <dd>{viewConfig.index_analyzer}</dd>
              <dt>Shards per Index:</dt>
              <dd>{viewConfig.shards}</dd>
              <dt>Replicas per Index:</dt>
              <dd>{viewConfig.replicas}</dd>
              <dt>Index optimization disabled:</dt>
              <dd>{viewConfig.index_optimization_disabled ? 'Yes' : 'No'}</dd>
              <dt>Max. Number of Segments:</dt>
              <dd>{viewConfig.index_optimization_max_num_segments}</dd>
              <dt>Field type refresh interval:</dt>
              <dd>{viewConfig.field_type_refresh_interval} {capitalize(viewConfig.field_type_refresh_interval_unit)}</dd>
              <br />
              <IndexMaintenanceStrategiesSummary config={rotationConfig(viewConfig)}
                                                 pluginExports={PluginStore.exports('indexRotationConfig')} />
              <IndexMaintenanceStrategiesSummary config={retentionConfig(viewConfig)}
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
                   aria-labelledby="dialog_label">
              <Formik onSubmit={saveConfig} initialValues={formConfig}>
                {({ values, setFieldValue, isSubmitting }) => (
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
                                                                     activeConfig={rotationConfig(formConfig)}
                                                                     getState={getRotationConfigState} />

                            <IndexMaintenanceStrategiesConfiguration title="Index Retention Configuration"
                                                                     name="retention"
                                                                     selectPlaceholder="Select rotation strategy"
                                                                     pluginExports={PluginStore.exports('indexRetentionConfig')}
                                                                     strategies={retentionStrategies.strategies}
                                                                     activeConfig={retentionConfig(formConfig)}
                                                                     getState={getRetentionConfigState} />
                          </Col>
                        </Row>
                      </div>
                    </Modal.Body>

                    <Modal.Footer>
                      <Button type="button" onClick={resetConfig}>Cancel</Button>
                      <Button type="submit"
                              bsStyle="success"
                              disabled={isSubmitting}>{isSubmitting ? 'Updating configuration' : 'Update configuration'}
                      </Button>
                    </Modal.Footer>
                  </Form>
                )}
              </Formik>
            </Modal>
          </>
        )}
      </div>
    </IndexRetentionProvider>
  );
};

export default IndexSetsDefaultsConfig;
