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
import styled from 'styled-components';
import { useDisclosure } from '@mantine/hooks';

import { Icon, Tooltip } from 'components/common';
import { Button } from 'components/bootstrap';

type Props = {
  title: string,
};

const IconWithHelp = styled(Icon)`
  cursor: help;
`;

const ClientAddressHead = ({ title }: Props) => {
  const [opened, { toggle }] = useDisclosure(false);

  return (
    <th>
      {title}
      <Tooltip opened={opened}
               withArrow
               label="The address of the client used to initially establish the session, not necessarily its current address.">
        <Button bsStyle="link" onClick={toggle}>
          <IconWithHelp name="help" />
        </Button>
      </Tooltip>
    </th>
  );
};

export default ClientAddressHead;
