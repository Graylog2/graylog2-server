// @flow strict
import * as React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';
import styled from 'styled-components';

type Props = {
  onChange: (event: SyntheticInputEvent<HTMLInputElement>, values: any) => void,
  onSubmit: (nextStepKey: string) => void,
  onSubmitAll: () => void,
  validateOnMount: boolean,
};

const Header = styled.h4`
  margin-bottom: 5px;
`;

const NoEnterpriseComponent = () => (
  <>
    <Header>No enterprise plugin found</Header>
    <p>To use the <b>Teams</b> functionality you need to install the Graylog <b>Enterprise</b> plugin.</p>
  </>
);

const GroupSyncSettings = ({ onSubmit, onSubmitAll, onChange, validateOnMount }: Props) => {
  const authenticationPlugin = PluginStore.exports('authentication.groupSync');

  if (!authenticationPlugin || authenticationPlugin.length <= 0) {
    return <NoEnterpriseComponent />;
  }

  const { GroupSyncForm } = authenticationPlugin[0];

  return (
    <GroupSyncForm onSubmit={onSubmit}
                   validateOnMount={validateOnMount}
                   onSubmitAll={onSubmitAll}
                   onChange={onChange} />
  );
};

export default GroupSyncSettings;
