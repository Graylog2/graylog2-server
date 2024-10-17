import React from 'react';
import moment from 'moment';
import styled from 'styled-components';

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
};

class SystemInformation extends React.Component<SystemInformationProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    jvmInformation: undefined,
  };

  private interval: NodeJS.Timeout;

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

  render() {
    const { systemInformation: { hostname, version, codename, timezone }, jvmInformation, node } = this.props;
    const { time } = this.state;
    let jvmInformationText;

    if (jvmInformation) {
      jvmInformationText = <span>PID {jvmInformation.pid}, {jvmInformation.info}</span>;
    } else {
      jvmInformationText = <span>JMV information for this node is unavailable.</span>;
    }

    return (
      <StyledDl>
        <dt>Hostname:</dt>
        <dd>{hostname}</dd>
        <dt>Node ID:</dt>
        <dd>{node.node_id}</dd>
        <dt>Version:</dt>
        <dd>{version}, codename <em>{codename}</em></dd>
        <dt>JVM:</dt>
        <dd>{jvmInformationText}</dd>
        <dt>Time:</dt>
        <dd><Timestamp dateTime={time} format="withTz" tz={timezone} /></dd>
      </StyledDl>
    );
  }
}

export default SystemInformation;
