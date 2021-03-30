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
// @flow strict
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';

import { getFullVersion } from 'util/Version';
import connect from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';

const SystemStore = StoreProvider.getStore('System');

type Props = {
  system?: {
    version: string,
    hostname: string,
  },
};

const StandardFooter = ({ system }: Props) => {
  const [jvm, setJvm] = useState();

  useEffect(() => {
    let mounted = true;

    SystemStore.jvm().then((jvmInfo) => {
      if (mounted) {
        setJvm(jvmInfo);
      }
    });

    return () => {
      mounted = false;
    };
  }, []);

  if (!(system && jvm)) {
    return (
      <>Graylog {getFullVersion()}</>
    );
  }

  return (
    <>
      Graylog {system.version} on {system.hostname} ({jvm.info})
    </>
  );
};

StandardFooter.propTypes = {
  system: PropTypes.shape({
    version: PropTypes.string,
    hostname: PropTypes.string,
  }),
};

StandardFooter.defaultProps = {
  system: undefined,
};

export default connect(StandardFooter, { system: SystemStore }, ({ system: { system } = {} }) => ({ system }));
