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
import { useState } from 'react';
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';
import { Form, Formik } from 'formik';
import type { PermissionsConfigType } from 'src/stores/configurations/ConfigurationsStore';

import { Button, Col, Modal, Row } from 'components/bootstrap';
import FormikInput from 'components/common/FormikInput';
import Spinner from 'components/common/Spinner';
import { InputDescription } from 'components/common';

type Props = {
  config: PermissionsConfigType,
  updateConfig: (config: PermissionsConfigType) => Promise<void>,
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

const LabelSpan = styled.span(({ theme }: { theme: DefaultTheme }) => css`
  margin-left: ${theme.spacings.sm};
  font-weight: bold;
`);

const PermissionsConfig = ({ config, updateConfig }: Props) => {
  const [showModal, setShowModal] = useState<boolean>(false);

  const _saveConfig = (values) => {
    updateConfig(values).then(() => {
      setShowModal(false);
    });
  };

  const _resetConfig = () => {
    setShowModal(false);
  };

  return (
    <div>
      <h2>Permissions Config</h2>
      <p>These settings can be used to control which entity sharing options are available.</p>

      {!config ? <Spinner /> : (
        <>
          <StyledDefList>
            <dt>Share with everyone:</dt>
            <dd>{config.allow_sharing_with_everyone ? 'Enabled' : 'Disabled'}</dd>
            <dt>Share with users:</dt>
            <dd>{config.allow_sharing_with_users ? 'Enabled' : 'Disabled'}</dd>
          </StyledDefList>

          <p>
            <Button type="button"
                    bsSize="xs"
                    bsStyle="info"
                    onClick={() => {
                      setShowModal(true);
                    }}>Configure
            </Button>
          </p>

          <Modal show={showModal} onHide={_resetConfig} aria-modal="true" aria-labelledby="dialog_label">
            <Formik onSubmit={_saveConfig} initialValues={config}>

              {({ isSubmitting }) => {
                return (
                  <Form>
                    <Modal.Header closeButton>
                      <Modal.Title id="dialog_label">Configure Permissions</Modal.Title>
                    </Modal.Header>

                    <Modal.Body>
                      <div>
                        <Row>
                          <Col sm={12}>
                            <FormikInput type="checkbox"
                                         name="allow_sharing_with_everyone"
                                         id="shareWithEveryone"
                                         label={(
                                           <LabelSpan>Share with everyone</LabelSpan>
                                         )} />
                            <InputDescription help="Control whether it is possible to share with everyone." />
                          </Col>
                          <Col sm={12}>
                            <FormikInput type="checkbox"
                                         name="allow_sharing_with_users"
                                         id="shareWithUsers"
                                         label={(
                                           <LabelSpan>Share with users</LabelSpan>
                                         )} />
                            <InputDescription help="Control whether it is possible to share with single users." />
                          </Col>

                        </Row>
                      </div>
                    </Modal.Body>

                    <Modal.Footer>
                      <Button type="button" bsStyle="link" onClick={_resetConfig}>Close</Button>
                      <Button type="submit" bsStyle="success" disabled={isSubmitting}>{isSubmitting ? 'Saving' : 'Save'}</Button>
                    </Modal.Footer>
                  </Form>
                );
              }}

            </Formik>
          </Modal>
        </>
      )}
    </div>
  );
};

export default PermissionsConfig;
