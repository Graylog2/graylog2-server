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
import React, { useEffect } from 'react';

import { DocumentTitle, PageHeader } from 'components/common';
import { InputsList } from 'components/inputs';
import { InputStatesStore } from 'stores/inputs/InputStatesStore';
import useCurrentUser from 'hooks/useCurrentUser';

const InputsPage = () => {
  const currentUser = useCurrentUser();

  useEffect(() => {
    const listInputsInterval = setInterval(InputStatesStore.list, 2000);

    return () => {
      clearInterval(listInputsInterval);
    };
  }, []);

  return (
    <DocumentTitle title="Inputs">
      <div>
        <PageHeader title="Inputs">
          <span>Graylog nodes accept data via inputs. Launch or terminate as many inputs as you want here.</span>
        </PageHeader>
        <InputsList permissions={currentUser.permissions} />
      </div>
    </DocumentTitle>
  );
};

export default InputsPage;
