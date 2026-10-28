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
import { MantineColors } from '../../mantine/types';
import { opacify } from '../../utils';

const textColors = (isLightTheme: boolean, colors: MantineColors, primaryTextColor: string) => ({
  primary: primaryTextColor,
  secondary: isLightTheme ? opacify(primaryTextColor, 0.6) : opacify(primaryTextColor, 0.6),
  disabled: isLightTheme ? opacify(primaryTextColor, 0.4) : opacify(primaryTextColor, 0.5),
  severity: {
    success: isLightTheme ? colors.success[6] : colors.success[5],
    danger: isLightTheme ? colors.success[5] : colors.success[4],
  },
});

export default textColors;
