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
import React, { useState } from 'react';
import styled, { css } from 'styled-components';

import { Panel } from 'components/bootstrap';
import useFetch from 'integrations/aws/common/hooks/useFetch';
import { ApiRoutes } from 'integrations/aws/common/Routes';
import Icon from 'components/common/Icon';

type PoliciesProps = {
  title: string;
  note: string;
  policy: any;
};

function Policies({
  title,
  note,
  policy,
}: PoliciesProps) {
  const [opened, setOpened] = useState(false);

  const toggleOpen = () => {
    setOpened(!opened);
  };

  return (
    <div>
      <Header onClick={toggleOpen}>
        <HeaderContent>
          <Title>{opened ? 'Hide' : 'Show'} {title}</Title>
          <Note>{note}</Note>
        </HeaderContent>

        <IconContainer $opened={opened}><Icon name="arrow_right_alt" size="2x" /></IconContainer>
      </Header>

      <Policy opened={opened}>
        {JSON.stringify(policy, null, 2)}
      </Policy>
    </div>
  );
}

export default function SidebarPermissions() {
  const [permissionsStatus] = useFetch(ApiRoutes.INTEGRATIONS.AWS.PERMISSIONS);

  return (
    <Panel bsStyle="info" header={<span>AWS Policy Permissions</span>}>
      <p>Please verify that you have granted your AWS IAM user sufficient permissions. You can use the following policies for reference.</p>

      {!permissionsStatus.loading && permissionsStatus.data && (
      <>
        <Policies title="Recommended Policy"
                  note="To be able to use all available functionality for Kinesis setup."
                  policy={JSON.parse(permissionsStatus.data.setup_policy)} />

        <Policies title="Least Privilege Policy"
                  note="Doesn&apos;t include Kinesis auto-subscription controls."
                  policy={JSON.parse(permissionsStatus.data.auto_setup_policy)} />
      </>
      )}
    </Panel>
  );
}

const Header = styled.header`
  display: flex;
  align-items: center;
  cursor: pointer;
`;

const HeaderContent = styled.div`
  flex-grow: 1;
`;

const IconContainer = styled.span<{ $opened: boolean }>(({ $opened }) => css`
  transform: rotate(${$opened ? '90deg' : '0deg'});
  transition: transform 150ms ease-in-out;
`);

const Policy = styled.pre<{ opened: boolean }>(({ opened }) => css`
  overflow: hidden;
  max-height: ${opened ? '1000px' : '0'};
  opacity: ${opened ? '1' : '0'};
  transition: max-height 150ms ease-in-out, opacity 150ms ease-in-out, margin 150ms ease-in-out, padding 150ms ease-in-out;
  margin-bottom: ${opened ? '12px' : '0'};
  padding: ${opened ? '9.5px' : '0'};
`);

const Title = styled.h4`
  font-weight: bold;
`;

const Note = styled.p`
  font-style: italic;
`;
