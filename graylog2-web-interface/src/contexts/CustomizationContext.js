// @flow strict
import * as React from 'react';
import { singleton } from '../views/logic/singleton';

export type CustomizationSetting = {
  [string]: boolean | string | number,
};

export type CustomizationType = {
  [string]: CustomizationSetting,
};

const defaultCustomization = {};

const CustomizationContext = React.createContext<CustomizationType>(defaultCustomization);
export default singleton('contexts.CustomizationContext', () => CustomizationContext);
