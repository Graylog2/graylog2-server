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
import styled, { css, useTheme } from 'styled-components';
import type { DefaultTheme } from 'styled-components';

import useConfigurationStep from 'preflight/hooks/useConfigurationStep';
import { CONFIGURATION_STEPS, CONFIGURATION_STEPS_ORDER } from 'preflight/Constants';
import type { IconName } from 'components/common/Icon';
import Icon from 'components/common/Icon';
import Spinner from 'components/common/Spinner';
import { List, Grid } from 'preflight/components/common';
import RenewalPolicyConfiguration from 'preflight/components/ConfigurationWizard/RenewalPolicyConfiguration';
import type { ConfigurationStep } from 'preflight/types';

import CertificateProvisioning from './CertificateProvisioning';
import CAConfiguration from './CAConfiguration';
import ConfigurationFinished from './ConfigurationFinished';

const StepIcon = styled(Icon)<{ $color: string }>(({ $color, theme }) => css`
  color: ${$color};
  background-color: ${theme.colors.input.background};
  border-radius: 50%;
`);

const stepIcon = (stepKey: ConfigurationStep, activeStepKey: ConfigurationStep, theme: DefaultTheme): { name: IconName, color: string } => {
  const stepIndex = CONFIGURATION_STEPS_ORDER.findIndex((key) => key === stepKey);
  const activeStepIndex = CONFIGURATION_STEPS_ORDER.findIndex((key) => key === activeStepKey);

  if (stepIndex < activeStepIndex || activeStepKey === CONFIGURATION_STEPS.CONFIGURATION_FINISHED.key) {
    return {
      name: 'circle-check',
      color: theme.colors.variant.success,
    };
  }

  if (stepKey === activeStepKey) {
    return {
      name: 'circle-exclamation',
      color: theme.colors.variant.info,
    };
  }

  return {
    name: 'circle',
    color: theme.colors.gray[90],
  };
};

type Props = {
  setIsWaitingForStartup: React.Dispatch<React.SetStateAction<boolean>>,
}

const ConfigurationWizard = ({ setIsWaitingForStartup }: Props) => {
  const { step: activeStepKey, isLoading: isLoadingConfigurationStep } = useConfigurationStep();
  const theme = useTheme();

  if (isLoadingConfigurationStep) {
    return <Spinner />;
  }

  return (
    <Grid>
      <Grid.Col span={12} md={6} orderMd={2}>
        <List spacing="md"
              size="lg"
              center>
          {CONFIGURATION_STEPS_ORDER.map((configurationStepKey) => {
            const { description } = CONFIGURATION_STEPS[configurationStepKey];
            const { name: iconName, color: iconColor } = stepIcon(configurationStepKey, activeStepKey, theme);

            return (
              <List.Item key={configurationStepKey} icon={<StepIcon name={iconName} $color={iconColor} size="xl" />}>
                {description}
              </List.Item>
            );
          })}
        </List>
      </Grid.Col>
      <Grid.Col span={12} md={6} orderMd={1}>
        {activeStepKey === CONFIGURATION_STEPS.CA_CONFIGURATION.key && <CAConfiguration />}
        {activeStepKey === CONFIGURATION_STEPS.RENEWAL_POLICY_CONFIGURATION.key && <RenewalPolicyConfiguration />}
        {activeStepKey === CONFIGURATION_STEPS.CERTIFICATE_PROVISIONING.key && <CertificateProvisioning />}
        {activeStepKey === CONFIGURATION_STEPS.CONFIGURATION_FINISHED.key && <ConfigurationFinished setIsWaitingForStartup={setIsWaitingForStartup} />}
      </Grid.Col>
    </Grid>
  );
};

export default ConfigurationWizard;
