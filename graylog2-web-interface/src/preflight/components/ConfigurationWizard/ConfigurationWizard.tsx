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
import { useCallback, useState } from 'react';

import useConfigurationStep from 'preflight/hooks/useConfigurationStep';
import { CONFIGURATION_STEPS, CONFIGURATION_STEPS_ORDER } from 'preflight/Constants';
import type { IconName } from 'components/common/Icon';
import Icon from 'components/common/Icon';
import Spinner from 'components/common/Spinner';
import { List, Grid, Space } from 'preflight/components/common';
import Alert from 'components/bootstrap/Alert';
import RenewalPolicyConfiguration from 'preflight/components/ConfigurationWizard/RenewalPolicyConfiguration';
import type { ConfigurationStep } from 'preflight/types';
import RestartConfigurationButton from 'preflight/components/RestartConfigurationButton';
import type FetchError from 'logic/errors/FetchError';

import CertificateProvisioning from './CertificateProvisioning';
import CAConfiguration from './CAConfiguration';
import ConfigurationFinished from './ConfigurationFinished';

const StepIcon = styled(Icon)<{ $color: string }>(({ $color, theme }) => css`
  color: ${$color};
  background-color: ${theme.colors.input.background};
  border-radius: 50%;
`);

const StyledListItem = styled(List.Item)<{ $isStepSkipped: boolean }>(({ $isStepSkipped }) => css`
  > * {
    text-decoration: ${$isStepSkipped ? 'line-through' : 'none'};
  }
`);

const stepIcon = (stepKey: ConfigurationStep, activeStepKey: ConfigurationStep, theme: DefaultTheme): { name: IconName, color: string } => {
  const stepIndex = CONFIGURATION_STEPS_ORDER.findIndex((key) => key === stepKey);
  const activeStepIndex = CONFIGURATION_STEPS_ORDER.findIndex((key) => key === activeStepKey);

  if (stepIndex < activeStepIndex || activeStepKey === CONFIGURATION_STEPS.CONFIGURATION_FINISHED.key) {
    return {
      name: 'check_circle',
      color: theme.colors.variant.success,
    };
  }

  if (stepKey === activeStepKey) {
    return {
      name: 'error',
      color: theme.colors.variant.info,
    };
  }

  return {
    name: 'circle',
    color: theme.colors.gray[90],
  };
};

type FetchErrorsOverviewProps = {
  errors: Array<{ entityName: string, error: FetchError}> | null
}

const FetchErrorsOverview = ({ errors }: FetchErrorsOverviewProps) => (
  <>
    {errors.map(({ entityName, error }) => (
      <Alert bsStyle="danger" key={entityName}>
        There was an error fetching the {entityName}: {error.message}
      </Alert>
    ))}
  </>
);

type Props = {
  setIsWaitingForStartup: React.Dispatch<React.SetStateAction<boolean>>,
}

const ConfigurationWizard = ({ setIsWaitingForStartup }: Props) => {
  const [isSkippingProvisioning, setIsSkippingProvisioning] = useState(false);
  const { step: activeStepKey, isLoading: isLoadingConfigurationStep, errors } = useConfigurationStep({ isSkippingProvisioning });
  const theme = useTheme();

  const onSkipProvisioning = useCallback(() => {
    setIsSkippingProvisioning(true);
  }, []);

  if (isLoadingConfigurationStep) {
    return <Spinner />;
  }

  if (errors?.length) {
    return <FetchErrorsOverview errors={errors} />;
  }

  return (
    <Grid>
      <Grid.Col span={{ base: 12, md: 6, orderMd: 2 }}>
        <List spacing="md"
              size="lg"
              center>
          {CONFIGURATION_STEPS_ORDER.map((configurationStepKey) => {
            const { description } = CONFIGURATION_STEPS[configurationStepKey];
            const isStepSkipped = configurationStepKey === CONFIGURATION_STEPS.CERTIFICATE_PROVISIONING.key && isSkippingProvisioning;
            const { name: iconName, color: iconColor } = stepIcon(configurationStepKey, activeStepKey, theme);

            return (
              <StyledListItem key={configurationStepKey}
                              $isStepSkipped={isStepSkipped}
                              icon={(
                                <StepIcon name={iconName}
                                          $color={iconColor}
                                          size="xl" />
                              )}>
                {description}
              </StyledListItem>
            );
          })}
        </List>
        <Space h="md" />
        You can always{' '}<RestartConfigurationButton compact variant="light" color="red" /> the configuration
      </Grid.Col>
      <Grid.Col span={{ base: 12, md: 6, orderMd: 1 }}>
        {activeStepKey === CONFIGURATION_STEPS.CA_CONFIGURATION.key && <CAConfiguration />}
        {activeStepKey === CONFIGURATION_STEPS.RENEWAL_POLICY_CONFIGURATION.key && <RenewalPolicyConfiguration />}
        {activeStepKey === CONFIGURATION_STEPS.CERTIFICATE_PROVISIONING.key && <CertificateProvisioning onSkipProvisioning={onSkipProvisioning} />}
        {activeStepKey === CONFIGURATION_STEPS.CONFIGURATION_FINISHED.key && (
          <ConfigurationFinished setIsWaitingForStartup={setIsWaitingForStartup}
                                 isSkippingProvisioning={isSkippingProvisioning}
                                 setIsSkippingProvisioning={setIsSkippingProvisioning} />
        )}
      </Grid.Col>
    </Grid>
  );
};

export default ConfigurationWizard;
