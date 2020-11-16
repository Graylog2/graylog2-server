// @flow strict

import * as React from 'react';

import AppConfig from '../AppConfig';

type HideOnCloudProps = {
  children: React.Node;
};

function HideOnCloud({ children }: HideOnCloudProps) {
  if (AppConfig.isCloud()) {
    return null;
  }

  return children;
}

export default HideOnCloud;
