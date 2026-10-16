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

import { Col } from 'components/bootstrap';
import ConfigletContainer from 'pages/configurations/ConfigletContainer';
import Spinner from 'components/common/Spinner';
import type { CoreSystemConfiguration } from 'views/types';

import RuntimeTunableConfiglet from './RuntimeTunableConfiglet';
import useFeatureTunables from './useFeatureTunables';
import type { RuntimeTunableFeatureSummary } from './types';

type CoreConfigurationProps = { ConfigurationComponent: React.ComponentType; title: string };

type Props = {
  feature: RuntimeTunableFeatureSummary;
  coreConfigurations: Array<CoreSystemConfiguration>;
};

const FeatureSection = ({ feature, coreConfigurations }: Props) => {
  const { tunables, isLoading } = useFeatureTunables(feature.id, { enabled: feature.tunableCount > 0 });

  return (
    <Col md={10}>
      {coreConfigurations.map(({ name, props }) => {
        const { ConfigurationComponent, title } = props as CoreConfigurationProps;

        return (
          <ConfigletContainer key={name} title={title}>
            <ConfigurationComponent />
          </ConfigletContainer>
        );
      })}
      {isLoading ? (
        <Spinner />
      ) : (
        tunables.map((tunable) => (
          <ConfigletContainer key={tunable.id} title={tunable.title}>
            <RuntimeTunableConfiglet tunable={tunable} featureId={feature.id} />
          </ConfigletContainer>
        ))
      )}
    </Col>
  );
};

export default FeatureSection;
