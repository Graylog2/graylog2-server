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
import { useQuery } from '@tanstack/react-query';

import useInputs from 'hooks/useInputs';
import usePluginEntities from 'hooks/usePluginEntities';

type Props = {
  value: string,
}

const useForwarderInput = (inputId: string, enabled: boolean) => {
  const { fetchForwarderInput } = usePluginEntities('forwarder')[0] ?? {};
  const { data: forwarderInput, isError, isLoading } = useQuery(
    ['forwarder', 'input', inputId],
    () => fetchForwarderInput(inputId),
    { enabled: fetchForwarderInput && enabled },
  );

  return (isLoading || isError) ? undefined : forwarderInput;
};

const InputField = ({ value }: Props) => {
  const inputsMap = useInputs();
  const forwarderInput = useForwarderInput(value, inputsMap && !inputsMap[value]);

  const inputTitle = inputsMap[value]?.title ?? forwarderInput?.title ?? value;

  return <span title={value}>{inputTitle}</span>;
};

export default InputField;
