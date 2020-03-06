// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import { Icon } from 'components/common';

const DitheredIcon: StyledComponent<{}, {}, HTMLElement> = styled(Icon)`
    opacity: 0.3;
    position: relative;
    top: 3px;
`;

const ReplaySearchButton = () => {
  return (
    <DitheredIcon name="play" />
  );
};

ReplaySearchButton.propTypes = {};

export default ReplaySearchButton;
