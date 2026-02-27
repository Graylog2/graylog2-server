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
import styled, { css } from 'styled-components';
import { Form, Formik } from 'formik';

import { Button, Col, Modal, Row } from 'components/bootstrap';
import FormikInput from 'components/common/FormikInput';
import { InputDescription, ModalSubmit, IfPermitted } from 'components/common';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import ConfigurationType from 'components/configurations/ConfigurationTypes';
import getConfig from 'components/configurations/helpers';
import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import reloadPage from 'preflight/components/reloadPage';
import type { MarkdownConfigType } from 'components/common/types';

import Spinner from '../common/Spinner';

const StyledDefList = styled.dl.attrs({ className: 'deflist' })(
  ({ theme }) => css`
    /* stylelint-disable-next-line nesting-selector-no-missing-scoping-root */
    &&.deflist {
      dd {
        padding-left: ${theme.spacings.md};
        margin-left: 400px;
      }
    }
  `,
);

const LabelSpan = styled.span(
  ({ theme }) => css`
    margin-left: ${theme.spacings.sm};
    font-weight: bold;
  `,
);

const configType = ConfigurationType.MARKDOWN_CONFIG;

const MarkdownConfig = () => {
  const [showModal, setShowModal] = useState<boolean>(false);
  const [viewConfig, setViewConfig] = useState<MarkdownConfigType | undefined>(undefined);
  const [formConfig, setFormConfig] = useState<MarkdownConfigType | undefined>(undefined);
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);

  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  useEffect(() => {
    ConfigurationsActions.list(configType).then(() => {
      const config = getConfig(configType, configuration) ?? {};

      setViewConfig(config);
      setFormConfig(config);
    });
  }, [configuration]);

  const saveConfig = (values: MarkdownConfigType) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.USER_UPDATED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'markdown',
      app_action_value: 'configuration-save',
    });
    ConfigurationsActions.update(configType, values).then(() => {
      if (
        values.allow_all_image_sources !== viewConfig.allow_all_image_sources ||
        values.allowed_image_sources !== viewConfig.allowed_image_sources
      ) {
        reloadPage();
      } else {
        setShowModal(false);
      }
    });
  };

  const resetConfig = () => {
    setShowModal(false);
    setFormConfig(viewConfig);
  };

  const modalTitle = 'Update Markdown Configuration';

  return (
    <div>
      <h2>Markdown Configuration</h2>
      <p>
        These settings can be used to configure Markdown rendering in different parts of the product, including the
        Text/Markdown widget. When changing settings for allowed image sources, the page will be reloaded afterwards to
        make sure they are applied.
      </p>

      {!viewConfig ? (
        <Spinner />
      ) : (
        <>
          <StyledDefList>
            <dt>Allow images from all sources:</dt>
            <dd>{viewConfig.allow_all_image_sources ? 'Enabled' : 'Disabled'}</dd>
            <dt>Allowed imaged sources (comma-separated):</dt>
            <dd>{viewConfig.allowed_image_sources || '-'}</dd>
          </StyledDefList>

          <IfPermitted permissions="clusterconfigentry:edit">
            <p>
              <Button
                type="button"
                bsSize="xs"
                bsStyle="info"
                onClick={() => {
                  setShowModal(true);
                }}>
                Edit configuration
              </Button>
            </p>
          </IfPermitted>

          <Modal show={showModal && !!formConfig} onHide={resetConfig}>
            <Formik onSubmit={saveConfig} initialValues={formConfig}>
              {({ isSubmitting, values }) => (
                <Form>
                  <Modal.Header>
                    <Modal.Title>{modalTitle}</Modal.Title>
                  </Modal.Header>

                  <Modal.Body>
                    <div>
                      <Row>
                        <Col sm={12}>
                          <FormikInput
                            type="checkbox"
                            name="allow_all_image_sources"
                            id="allow_all_image_sources"
                            label={<LabelSpan>Allow images from all sources</LabelSpan>}
                          />
                          <InputDescription help="If enabled, images can be embedded from all sources." />
                        </Col>
                        <Col sm={12}>
                          <FormikInput
                            type="text"
                            name="allowed_image_sources"
                            id="allowed_image_sources"
                            disabled={values.allow_all_image_sources === true}
                            label={<LabelSpan>Allowed images sources (comma-separated)</LabelSpan>}
                          />
                          <InputDescription help="Allowed image sources for image embedding markdown documents." />
                        </Col>
                      </Row>
                    </div>
                  </Modal.Body>

                  <Modal.Footer>
                    <ModalSubmit
                      onCancel={resetConfig}
                      isSubmitting={isSubmitting}
                      isAsyncSubmit
                      submitLoadingText="Update configuration"
                      submitButtonText="Update configuration"
                    />
                  </Modal.Footer>
                </Form>
              )}
            </Formik>
          </Modal>
        </>
      )}
    </div>
  );
};

export default MarkdownConfig;
