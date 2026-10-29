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
import moment from 'moment';
import styled from 'styled-components';
import { Trans, withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';

import { Timestamp } from 'components/common';

const _getInitialState = () => ({ time: moment() });

const StyledDl = styled.dl`
  margin-top: 5px;
  margin-bottom: 0;

  dt {
    float: left;
  }

  dd {
    margin-left: 95px;
  }
`;

type SystemInformationProps = {
  node: any;
  systemInformation: any;
  jvmInformation?: any;
} & WithTranslation;

class SystemInformation extends React.Component<
  SystemInformationProps,
  {
    [key: string]: any;
  }
> {
  static defaultProps = {
    jvmInformation: undefined,
  };

  constructor(props) {
    super(props);
    this.state = _getInitialState();
  }

  componentDidMount() {
    this.interval = setInterval(() => this.setState(_getInitialState()), 1000);
  }

  componentWillUnmount() {
    clearTimeout(this.interval);
  }

  private interval: NodeJS.Timeout;

  render() {
    const {
      systemInformation: { hostname, version, codename, timezone },
      jvmInformation,
      node,
      t,
    } = this.props;
    const { time } = this.state;
    let jvmInformationText;

    if (jvmInformation) {
      jvmInformationText = <span>{t('system_information.pid_info', { pid: jvmInformation.pid, info: jvmInformation.info })}</span>;
    } else {
      jvmInformationText = <span>{t('system_information.jvm_unavailable')}</span>;
    }

    return (
      <StyledDl>
        <dt>{t('system_information.hostname')}</dt>
        <dd>{hostname}</dd>
        <dt>{t('system_information.node_id')}</dt>
        <dd>{node.node_id}</dd>
        <dt>{t('system_information.version')}</dt>
        <dd>
          <Trans
            t={t}
            i18nKey="system_information.version_codename"
            values={{ version, codename }}
            components={{ em: <em /> }}
          />
        </dd>
        <dt>{t('system_information.jvm')}</dt>
        <dd>{jvmInformationText}</dd>
        <dt>{t('system_information.time')}</dt>
        <dd>
          <Timestamp dateTime={time} format="withTz" tz={timezone} />
        </dd>
      </StyledDl>
    );
  }
}

export default withTranslation('system')(SystemInformation);
