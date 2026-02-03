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

import { Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import AppConfig from 'util/AppConfig';
import { Link } from 'components/common/router';
import DocsHelper from 'util/DocsHelper';
import useProductName from 'brand-customization/useProductName';
import { InputsOverview } from 'components/inputs/InputsOveriew';
import useInputTypes from 'hooks/useInputTypes';
import useInputTypesDescriptions from 'hooks/useInputTypesDescriptions';
import InputsNotifications from 'components/inputs/InputsNotifications';

const isCloud = AppConfig.isCloud();

const InputsPage = () => {
  const productName = useProductName();
  const { data: inputTypes, isLoading: isLoadingInputTypes } = useInputTypes();
  const { data: inputTypeDescriptions, isLoading: isLoadingInputTypesDescriptions } = useInputTypesDescriptions();

  if (isLoadingInputTypes || isLoadingInputTypesDescriptions) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title="Inputs">
      <PageHeader
        title="Inputs"
        documentationLink={{
          title: 'Inputs documentation',
          path: DocsHelper.PAGES.INPUTS,
        }}>
        {isCloud ? (
          <>
            <p>
              {' '}
              {productName} cloud accepts data via inputs. There are many types of inputs to choose from, but only some
              can run directly in the cloud. You can launch and terminate them on this page.
            </p>
            <p>
              If you are missing an input type on this page&apos;s list of available inputs, you can start the input on
              a <Link to="/system/forwarders">Forwarder</Link>.
            </p>
          </>
        ) : (
          <span>{productName} nodes accept data via inputs. Launch or terminate as many inputs as you want here.</span>
        )}
      </PageHeader>
      <InputsNotifications />
      <Row className="content">
        <Col md={12}>
          <InputsOverview inputTypeDescriptions={inputTypeDescriptions} inputTypes={inputTypes} />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default InputsPage;
