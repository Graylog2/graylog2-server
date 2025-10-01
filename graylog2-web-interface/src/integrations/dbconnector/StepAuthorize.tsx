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
import React, { useEffect, useContext, useState } from "react";
import styled from 'styled-components';

import { qualifyUrl } from "util/URLUtils";
import fetch from "logic/rest/FetchProvider";
import { Button, ControlLabel, FormControl, HelpBlock } from 'components/bootstrap';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';

import { ApiRoutes } from "./Routes";

import type {
  ErrorMessageType,
  FormDataContextType,
  HandleSubmitType,
} from '../common/utils/types';
import { renderOptions } from '../common/Options';
import FormWrap from '../common/FormWrap';
import { FormDataContext } from '../common/context/FormData';
import formValidation from '../common/utils/formValidation';
import ValidatedInput from '../common/ValidatedInput';



const FieldRow = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 0px;
`;

const FieldColumn = styled.div`
  flex: 1 1 180px;
  display: flex;
  flex-direction: column;
`;

type StepAuthorizeProps = {
  onSubmit: HandleSubmitType;
  onChange: (...args: any[]) => void;
};

const defaultPorts = {
  Oracle: '1521',
  MySQL: '3306',
  'Microsoft SQL': '1433',
  MongoDB: '27017',
  DB2: '50000',
  PostgreSQL: '5432',
};

const StepAuthorize = ({ onSubmit, onChange }: StepAuthorizeProps) => {
  const { formData } = useContext<FormDataContextType>(FormDataContext);
  const [formError, setFormError] = useState<ErrorMessageType>(null);
  const { connectionString } = formData;
  const [name, setName] = useState<any>();
  const [showModal, setShowModal] = useState(false);
  const database = [
    { label: 'Oracle', value: 'Oracle' },
    { label: 'MySQL', value: 'MySQL' },
    { label: 'Microsoft SQL Server', value: 'Microsoft SQL' },
    { label: 'MongoDB', value: 'MongoDB' },
    { label: 'DB2', value: 'DB2' },
    { label: 'PostgreSQL', value: 'PostgreSQL' },
  ];
  const selectedDbType = formData.dbType?.value;

  const testButtonText = 'Run Test';
  const handleSubmit = () => {
    onSubmit();
  };

  const _modalContent = () => {
    if (name) {
      return (
        <ul>
          {Object.entries(name).map(([key, value]) => (
            <li key={key}>
              <strong>{key}</strong>:&nbsp;
              {typeof value === 'object' ? (
                <pre style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                  {JSON.stringify(value, null, 2)}
                </pre>
              ) : (
                value?.toString()
              )}
            </li>
          ))}
        </ul>
      );
    }

    return null;
  };

  const handleCloseModal = () => {
    setShowModal(false);
  };

  const handleSubmitModal = () => {
    setShowModal(false);
  };

  const verifyConnection = () => {
    fetch(
      "POST",
      qualifyUrl(ApiRoutes.INTEGRATIONS.DBConnector.TEST_INPUT),
      {
        hostname: formData?.hostname?.value || "",
        port: formData?.port?.value || "",
        database_name: formData?.databaseName?.value || "",
        username: formData?.username?.value || "",
        password: formData?.password?.value || "",
        db_type: formData?.dbType?.value || "",
        table_name: formData?.tableName.value || "",
        mongo_collection_name: formData?.mongoCollectionName.value || "",
      })
      .then((result: any) => {
        setName(result);
        setShowModal(true);
      })
      .catch((err) => {
        setFormError({
          full_message: err.message,
          nice_message:
            "Unable to connect to your Database using provided configuration.",
        });
      });
  };

  const isFormValid = formValidation.isFormValid(
    ["dbConnectorName", "dbType", "hostname", "port", "databaseName", "username", "password"],
    formData
  );

  useEffect(() => {
    if (selectedDbType && defaultPorts[selectedDbType]) {
      onChange({
        target: {
          name: 'port',
          value: defaultPorts[selectedDbType],
        },
      });
    }
  }, [selectedDbType, onChange]);



  return (

    <div>

      <BootstrapModalForm
        show={showModal}
        onCancel={handleCloseModal}
        onSubmitForm={handleSubmitModal}
        title="Query Results"
        submitButtonText="Close"
        submitButtonDisabled
        data-telemetry-title="Test DB Connection"
      >

        {_modalContent()}
      </BootstrapModalForm>

      <FormWrap onSubmit={handleSubmit}
        buttonContent="Verify Connection &amp; Proceed"
        disabled={isFormValid}
        title=""
        className="col-md-7"
        error={formError}
        description="">

        <ValidatedInput id="dbConnectorName"
          type="text"
          fieldData={formData.dbConnectorName}
          onChange={onChange}
          placeholder="Input Name"
          label="Input Name"
          autoComplete="off"
          help="Provide a name for your database connector."
          defaultValue={connectionString?.value}
          required />

        <ValidatedInput
          type="select"
          id="dbType"
          onChange={onChange}
          fieldData={formData.dbType}
          help="The Database type from which logs are to be pulled."
          required
          label="Database type">
          {renderOptions(database, 'Choose your database type', false)}
        </ValidatedInput>

        <FieldRow>
          <FieldColumn>
            <ValidatedInput
              id="hostname"
              type="text"
              fieldData={formData.hostname}
              onChange={onChange}
              label="Hostname"
              help="Database host or IP address"
              required
            />
          </FieldColumn>

          <FieldColumn>
            <ValidatedInput
              id="port"
              type="text"
              fieldData={formData.port}
              onChange={onChange}
              label="Port"
              help="Database port"
              required
            />
          </FieldColumn>
        </FieldRow>

        <FieldRow>
          <FieldColumn>
            <ValidatedInput
              id="databaseName"
              type="text"
              fieldData={formData.databaseName}
              onChange={onChange}
              label="Database Name"
              help="Name of the database"
              required
            />
          </FieldColumn>

          <FieldColumn>
            {selectedDbType === "MongoDB" ? (
              <ValidatedInput
                id="mongoCollectionName"
                type="text"
                onChange={onChange}
                fieldData={formData.mongoCollectionName}
                help="The collection name of the Mongo database"
                label="MongoDB Collection Name"
                required
              />
            ) : (
              <ValidatedInput
                id="tableName"
                type="text"
                onChange={onChange}
                fieldData={formData.tableName}
                help="Name of the table containing the logs"
                label="Table Name"
                required
              />
            )}
          </FieldColumn>
        </FieldRow>

        <FieldRow>
          <FieldColumn>
            <ValidatedInput
              id="username"
              type="text"
              fieldData={formData.username}
              onChange={onChange}
              label="Username"
              help="Database username"
              required
            />
          </FieldColumn>

          <FieldColumn>
            <ValidatedInput
              id="password"
              type="password"
              fieldData={formData.password}
              onChange={onChange}
              label="Password"
              help="Database password"
              required
            />
          </FieldColumn>
        </FieldRow>

        <ControlLabel>Test Connection <small className="text-muted">(Optional)</small></ControlLabel>
        <FormControl.Static>
          <Button bsStyle="info"
            bsSize="small"
            onClick={verifyConnection}>

            {testButtonText}

          </Button>
        </FormControl.Static>
        <HelpBlock>
          Execute this test query statement.
        </HelpBlock>


      </FormWrap>
    </div>
  );
};


export default StepAuthorize;