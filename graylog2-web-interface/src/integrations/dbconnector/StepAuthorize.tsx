import React, { useContext, useState } from "react";
import { qualifyUrl } from "util/URLUtils";
import fetch from "logic/rest/FetchProvider";

import styled from 'styled-components';

import type {
  ErrorMessageType,
  HandleFieldUpdateType,
  FormDataContextType,
  HandleSubmitType,
} from '../common/utils/types';
import { renderOptions } from '../common/Options';
import FormWrap from '../common/FormWrap';
import { FormDataContext } from '../common/context/FormData';
import formValidation from '../common/utils/formValidation';
import ValidatedInput from '../common/ValidatedInput';

import { ApiRoutes } from "./Routes";
import { Button, ControlLabel, FormControl, HelpBlock } from 'components/bootstrap';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';


const FieldRow = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 12px;
`;

const FieldColumn = styled.div`
  flex: 1 1 180px;
  display: flex;
  flex-direction: column;
`;

type StepAuthorizeProps = {
  onSubmit: HandleSubmitType;
  onChange: HandleFieldUpdateType;
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
    console.log(formData?.dbType.value)
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
        console.log(name);
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

  const selectedDbType = formData?.dbType?.value;


  return (

    <div>

      <BootstrapModalForm
        show={showModal}
        onCancel={handleCloseModal}
        onSubmitForm={handleSubmitModal}
        title="Query Results"
        submitButtonText="Close"
        submitButtonDisabled={true}
        data-telemetry-title="Test DB Connection"
      >

        {_modalContent()}
      </BootstrapModalForm>

      <FormWrap onSubmit={handleSubmit}
        buttonContent="Verify Connection &amp; Proceed"
        disabled={isFormValid}
        title=""
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

        {selectedDbType === "MongoDB" && (
          <>
            <ValidatedInput
              id="mongoCollectionName"
              type="text"
              onChange={onChange}
              fieldData={formData.mongoCollectionName}
              help="The collection name of the Mongo database"
              label="MongoDB Collection Name"
              required
            />
          </>
        )}

        {selectedDbType && selectedDbType !== "MongoDB" && (
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

        {/* {logTypeVal === "MongoDB" &&

          <>
            <ValidatedInput id="mongoDatabaseName"
              type="text"
              onChange={onChange}
              fieldData={formData.mongoDatabaseName}
              help="The Database name of the Mongo repository"
              label="Mongo Database Name"
              required />
            <ValidatedInput id="mongoCollectionName"
              type="text"
              onChange={onChange}
              fieldData={formData.mongoCollectionName}
              help="The collection name of the Mongo database"
              label="MongoDB Collection Name"
              required />

          </>

        }
        {logTypeVal && logTypeVal != "MongoDB" &&
          <ValidatedInput id="tableName"
            type="text"
            onChange={onChange}
            fieldData={formData.tableName}
            help="Name of the table containing the logs"
            label="Table Name"
            required />
        } */}

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