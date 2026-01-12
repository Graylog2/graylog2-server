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

/**
 * Helper function to set all four border-side colors to the same value.
 * This is needed to avoid conflicts with Mantine 8.3.0+ which internally
 * uses specific border-side properties. Using the shorthand `borderColor`
 * can cause React warnings about mixing shorthand and non-shorthand properties.
 *
 * @param color - The color to apply to all four border sides
 * @returns An object with all four border-side color properties set
 */
export const borderColor = (color: string) => ({
  borderTopColor: color,
  borderRightColor: color,
  borderBottomColor: color,
  borderLeftColor: color,
});
