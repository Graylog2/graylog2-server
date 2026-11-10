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
import { useQuery } from '@tanstack/react-query';

import { getFullVersion } from 'util/Version';
import useProductName from 'brand-customization/useProductName';
import useSystemDetails from 'hooks/useSystemDetails';
import { fetchSystemJvm } from 'hooks/useSystemStore';

const StandardFooter = () => {
  const productName = useProductName();
  const { data: jvm } = useQuery({ queryKey: ['system', 'jvm'], queryFn: fetchSystemJvm });
  const system = useSystemDetails();

  return !(system && jvm) ? (
    <>
      {productName} {getFullVersion()}
    </>
  ) : (
    <>
      {productName} {system.version} on {system.hostname} ({jvm.info})
    </>
  );
};

export default StandardFooter;
