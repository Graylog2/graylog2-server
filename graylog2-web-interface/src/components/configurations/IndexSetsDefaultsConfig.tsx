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
import React from 'react';

import {Form, Formik} from 'formik';
import type {DefaultTheme} from 'styled-components';
import styled, {css} from 'styled-components';
import {useState} from 'react';

import {IfPermitted, InputDescription} from 'components/common';
import {Button, Col, Modal, Row} from 'components/bootstrap';
import FormikInput from '../common/FormikInput';

type IndexConfig = {
  indexPrefix: string,
  indexAnalyzer: String,
  shards: number,
  replicas: number,
  indexOptimizationMaxNumSegments: number,
  indexOptimizationDisabled: boolean,
  fieldTypeRefreshInterval: number,
}

type Props = {
  config: IndexConfig,
  updateConfig: (arg: IndexConfig) => void,
};

const LabelSpan = styled.span(({theme}: { theme: DefaultTheme }) => css`
  margin-left: ${theme.spacings.sm};
  font-weight: bold;
`);

const IndexSetsDefaultsConfig = ({config, updateConfig}: Props) => {

  const [showModal, setShowModal] = useState<boolean>(false);

  const handleSaveConfig = async (config: IndexConfig) => updateConfig(config);

  const _saveConfig = (values) => {
    handleSaveConfig(values).then(() => {
      setShowModal(false);
    });
  };

  const _resetConfig = () => {
    setShowModal(false);
  };

  return (
    <div>
      <h2>Index Defaults</h2>
      <p>Defaults for newly created index sets.</p>
      <dl className="deflist">
        <dt>Index prefix:</dt>
        <dd>{config.indexPrefix}</dd>
        <dt>Index analyzer:</dt>
        <dd>{config.indexAnalyzer}</dd>
        <dt>Shards per Index:</dt>
        <dd>{config.shards}</dd>
        <dt>Replicas per Index:</dt>
        <dd>{config.replicas}</dd>
        <dt>Max. Number of Segments:</dt>
        <dd>{config.indexOptimizationMaxNumSegments}</dd>
        <dt>Index optimization disabled:</dt>
        <dd>{config.indexOptimizationDisabled}</dd>
        <dt>Field type refresh interval</dt>
        <dd>{config.fieldTypeRefreshInterval}</dd>
      </dl>

      <p>
        <IfPermitted permissions="clusterconfigentry:edit">
          <Button
            bsStyle="info"
            bsSize="xs"
            onClick={() => {
              setShowModal(true);
            }}>Update</Button>
        </IfPermitted>
      </p>
      {/* TODO: Remove the this, because not class */}
      {/* TODO: Don't use the _underscore function name. Just use name */}
      {/* TODO: Always define the function as a variable. Always start with const =  */}
      {/* TODO: const funcName = (name: type) => {} */}
      {/* TODO: Want to use functional component with hook */}

      <Modal show={showModal} onHide={_resetConfig} aria-modal="true" aria-labelledby="dialog_label">
        <Formik onSubmit={_saveConfig} initialValues={config}>
          {({isSubmitting}) => {
            return (
              <Form>
                <Modal.Header closeButton>
                  <Modal.Title id="dialog_label">Configure Index Set Defaults</Modal.Title>
                </Modal.Header>

                <Modal.Body>
                  <div>
                    <Row>
                      <Col sm={12}>
                        <FormikInput label="Index Prefix"
                                     name="indexPrefix"
                                     id="index-prefix"
                                     help="The prefix."
                                     required />
                        <InputDescription help={<>A relevant description</>} />
                      </Col>
                    </Row>
                  </div>
                </Modal.Body>

                <Modal.Footer>
                  <Button type="button" bsStyle="link" onClick={_resetConfig}>Close</Button>
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
};

export default IndexSetsDefaultsConfig;
