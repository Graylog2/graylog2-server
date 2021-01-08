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
const reds = ['#b71c1c', '#ce5246', '#e27c72', '#f3a4a1', '#ffcdd2'];
const pinks = ['#880e4f', '#a5426d', '#c16b8d', '#dd93ae', '#f8bbd0'];
const purples = ['#4a148c', '#7240a3', '#9869b9', '#bc93d0', '#e1bee7'];
const darkpurples = ['#311b92', '#6044a8', '#876dbe', '#ad98d3', '#d1c4e9'];

const darkblues = ['#1a237e', '#4e4998', '#7772b3', '#9e9dce', '#c5cae9'];
const blues = ['#0d47a1', '#4b6ab7', '#738fce', '#97b5e4', '#bbdefb'];
const lightblues = ['#01579b', '#4478b3', '#6b9bcb', '#8fbfe3', '#b3e5fc'];
const cyans = ['#006064', '#3a8185', '#62a3a8', '#8ac6cc', '#b2ebf2'];

const darkgreens = ['#004d40', '#356f64', '#5e9389', '#87b8b1', '#b2dfdb'];
const greens = ['#194d33', '#447155', '#6e967a', '#9abda1', '#c8e6c9'];
const lightgreens = ['#33691e', '#5d8947', '#87a970', '#b1cb9b', '#dcedc8'];
const dirtyyellow = ['#827717', '#9e9544', '#bab36d', '#d5d398', '#f0f4c3'];

const lightorange = ['#f57f17', '#fd9e48', '#ffbe73', '#ffdc9c', '#fff9c4'];
const orange = ['#ff6f00', '#ff943f', '#ffb368', '#ffd08e', '#ffecb3'];
const darkorange = ['#e65100', '#f17837', '#fa9c5f', '#febe88', '#ffe0b2'];
const darkred = ['#bf360c', '#d35f39', '#e58463', '#f4a88f', '#ffccbc'];

const brown = ['#3e2723', '#624c48', '#877470', '#af9f9b', '#d7ccc8'];
const gray = ['#263238', '#4c575d', '#758085', '#a1abb0', '#cfd8dc'];
const black = ['#000000', '#3b3b3b', '#777777', '#b9b9b9', '#ffffff'];

export const colors = [
  reds, pinks, purples, darkpurples,
  darkblues, blues, lightblues, cyans,
  darkgreens, greens, lightgreens, dirtyyellow,
  lightorange, orange, darkorange, darkred,
  brown, gray, black,
];

export const defaultChartColors = [
  lightblues[1],
  lightorange[1],
  lightgreens[1],
  reds[1],
  purples[1],
  brown[1],
  pinks[1],
  lightorange[3],
  cyans[3],
  darkblues[0],
  darkorange[0],
  darkgreens[0],
  darkpurples[0],
  gray[2],
  purples[4],
  darkred[0],
];
