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
import { useEffect, useState, useRef } from 'react';

import { NodeOrGlobalSelect } from 'components/inputs';
import { ConfigurationForm } from 'components/configurationforms';
import HideOnCloud from 'util/conditional/HideOnCloud';
import AppConfig from 'util/AppConfig';
import type { Input } from 'components/messageloaders/Types';
import type { ConfigurationField } from 'components/configurationforms';

type FormValues = Input['attributes'];

type Props = {
  globalValue?: boolean,
  configFields: {
    [key: string]: ConfigurationField,
  },
  nodeValue?: string,
  titleValue?: string,
  typeName: string,
  title: string,
  includeTitleField: boolean,
  handleSubmit: (data: any) => void,
  values?: FormValues,
  setShowModal: (boolean) => void;
  submitButtonText: string,
}

const InputForm = ({
  globalValue,
  configFields,
  nodeValue,
  titleValue,
  title,
  typeName,
  includeTitleField,
  handleSubmit,
  values,
  setShowModal,
  submitButtonText,
} : Props) => {
  const [global, setGlobal] = useState<boolean>(globalValue ?? false);
  const [node, setNode] = useState<string | undefined>(nodeValue);
  const configFormRef = useRef(null);

  useEffect(() => {
    configFormRef.current?.open();
  }, []);

  const handleChange = (type: 'node' | 'global', value: boolean | string | undefined | null) => {
    if (type === 'node') {
      setNode(value as unknown as string | undefined);
    }

    if (type === 'global') {
      setGlobal(value as unknown as boolean);
    }
  };

  const onSubmit = (data: FormValues) => {
    const newData = {
      ...data,
      ...{
        global: AppConfig.isCloud() || global,
        node: node,
      },
    };

    handleSubmit(newData);

    setShowModal(false);
  };

  const onCancel = () => {
    setShowModal(false);
  };

  const getValues = () => {
    if (values) {
      return values;
    }

    if (configFormRef.current) {
      return configFormRef.current.getValue().configuration;
    }

    return {};
  };

  const getTitleValue = () => {
    if (titleValue) {
      return titleValue;
    }

    if (configFormRef.current) {
      return configFormRef.current.getValue().titleValue;
    }

    return '';
  };

  const formValues = getValues();
  const formTitleValue = getTitleValue();

  return (
    <ConfigurationForm<FormValues> ref={configFormRef}
                                   configFields={configFields}
                                   includeTitleField={includeTitleField}
                                   title={title}
                                   values={formValues}
                                   titleValue={formTitleValue}
                                   submitButtonText={submitButtonText}
                                   submitAction={onSubmit}
                                   typeName={typeName}
                                   cancelAction={onCancel}>
      <HideOnCloud>
        <NodeOrGlobalSelect onChange={handleChange} global={global} node={node} />
      </HideOnCloud>
    </ConfigurationForm>
  );
};

export default InputForm;

InputForm.defaultProps = {
  globalValue: false,
  nodeValue: undefined,
  titleValue: undefined,
  values: undefined,
};
