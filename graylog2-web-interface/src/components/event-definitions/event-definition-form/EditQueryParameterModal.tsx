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
import React, { useCallback, useState } from 'react';

import { Button, BootstrapModalForm, Input } from 'components/bootstrap';
import usePluginEntities from 'hooks/usePluginEntities';
import type Parameter from 'views/logic/parameters/Parameter';
import type { ParameterJson } from 'views/logic/parameters/Parameter';

type Props = {
  queryParameters: Array<ParameterJson>;
  onChange: (newQueryParameters: Array<ParameterJson>) => void;
  queryParameter: Parameter;
  embryonic: boolean;
};

const EMPTY_PARAM_JSON: Omit<ParameterJson, 'type'> = {
  name: '',
  title: 'new title',
  description: '',
  data_type: 'any',
  default_value: null,
  optional: false,
  binding: null,
};

const EditQueryParameterModal = ({ queryParameters, onChange, queryParameter: initialParameter, embryonic }: Props) => {
  const [showModal, setShowModal] = useState(false);
  const [parameter, setParameter] = useState<Parameter>(initialParameter);
  const [validation, setValidation] = useState<Record<string, string | undefined>>({});

  const parameterTypes = usePluginEntities('eventDefinitionQueryParameterTypes');
  const currentTypeDef = parameterTypes.find((t) => t.type === parameter.type) ?? parameterTypes[0];

  const openModal = useCallback(() => {
    setParameter(initialParameter);
    setValidation({});
    setShowModal(true);
  }, [initialParameter]);

  const handleClose = useCallback(() => {
    setParameter(initialParameter);
    setShowModal(false);
  }, [initialParameter]);

  const handleTypeChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const newType = e.target.value;
      const newTypeDef = parameterTypes.find((t) => t.type === newType);
      if (!newTypeDef) return;

      const newParam = newTypeDef.fromJSON({
        ...EMPTY_PARAM_JSON,
        name: parameter.name,
        title: parameter.title ?? 'new title',
        description: parameter.description ?? '',
        type: newType,
      });

      setParameter(newParam);
      setValidation({});
    },
    [parameter, parameterTypes],
  );

  const handleChange = useCallback((key: string, value: any) => {
    setParameter((prev) => (prev as any).toBuilder()[key](value).build());
  }, []);

  const handleSave = useCallback(() => {
    if (!currentTypeDef) return;

    const errors = currentTypeDef.validate(parameter);
    setValidation(errors);

    if (Object.values(errors).some(Boolean)) return;

    const newQueryParameters = [...queryParameters];
    const index = queryParameters.findIndex((p) => p.name === initialParameter.name);

    if (index < 0) throw new Error(`Query parameter "${initialParameter.name}" not found`);

    newQueryParameters[index] = (parameter as any).toJSON();
    onChange(newQueryParameters);
    setShowModal(false);
  }, [currentTypeDef, initialParameter.name, onChange, parameter, queryParameters]);

  if (parameterTypes.length === 0) {
    return null;
  }

  const validationState = Object.fromEntries(
    Object.entries(validation).map(([k, v]) => [k, v ? (['error', v] as ['error', string]) : undefined]),
  );

  const EditComponent = currentTypeDef?.editComponent;

  return (
    <>
      <Button bsSize="small" bsStyle={embryonic ? 'primary' : 'info'} onClick={openModal}>
        {initialParameter.name}
        {embryonic && ': undeclared'}
      </Button>

      <BootstrapModalForm
        show={showModal}
        title={`Declare Query Parameter "${initialParameter.name}"`}
        data-telemetry-title="Declare Query Parameter"
        onSubmitForm={handleSave}
        onCancel={handleClose}
        submitButtonText="Save">
        {parameterTypes.length > 1 && (
          <Input
            id={`parameter-type-${initialParameter.name}`}
            type="select"
            label="Type"
            value={parameter.type}
            onChange={handleTypeChange}>
            {parameterTypes.map(({ type, title }) => (
              <option key={type} value={type}>
                {title}
              </option>
            ))}
          </Input>
        )}
        {EditComponent && (
          <EditComponent
            parameter={parameter}
            onChange={handleChange}
            identifier={initialParameter.name}
            validationState={validationState}
          />
        )}
      </BootstrapModalForm>
    </>
  );
};

export default EditQueryParameterModal;
