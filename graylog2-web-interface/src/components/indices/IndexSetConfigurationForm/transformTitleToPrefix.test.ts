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
import { transformTitleToPrefix } from './IndexSetConfigurationForm';

describe('transformTitleToPrefix', () => {
  it('converts all letters to lowercase', () => {
    expect(transformTitleToPrefix('MY INDEX')).toBe('my-index');
    expect(transformTitleToPrefix('MyIndex')).toBe('myindex');
    expect(transformTitleToPrefix('INDEX123')).toBe('index123');
    expect(transformTitleToPrefix('myindex')).toBe('myindex');
  });

  it('removes leading underscores, hyphens, and plus signs', () => {
    expect(transformTitleToPrefix('_myindex')).toBe('myindex');
    expect(transformTitleToPrefix('-myindex')).toBe('myindex');
    expect(transformTitleToPrefix('+myindex')).toBe('myindex');
    expect(transformTitleToPrefix('___myindex')).toBe('myindex');
    expect(transformTitleToPrefix('---myindex')).toBe('myindex');
    expect(transformTitleToPrefix('+++myindex')).toBe('myindex');
    expect(transformTitleToPrefix('_-+_-+myindex')).toBe('myindex');
  });

  it('replaces prohibited characters with hyphen', () => {
    expect(transformTitleToPrefix('my index')).toBe('my-index');
    expect(transformTitleToPrefix('my,index')).toBe('my-index');
    expect(transformTitleToPrefix('namespace:index')).toBe('namespace-index');
    expect(transformTitleToPrefix('my"index"name')).toBe('my-index-name');
    expect(transformTitleToPrefix('my*index')).toBe('my-index');
    expect(transformTitleToPrefix('logs/2024')).toBe('logs-2024');
    expect(transformTitleToPrefix('my\\index')).toBe('my-index');
    expect(transformTitleToPrefix('my|index')).toBe('my-index');
    expect(transformTitleToPrefix('my?index')).toBe('my-index');
    expect(transformTitleToPrefix('my#index')).toBe('my-index');
    expect(transformTitleToPrefix('my>index')).toBe('my-index');
    expect(transformTitleToPrefix('my<index')).toBe('my-index');
  });

  it('collapses consecutive prohibited characters into single hyphen', () => {
    expect(transformTitleToPrefix('my    index')).toBe('my-index');
    expect(transformTitleToPrefix('my@#$index')).toBe('my-index');
    expect(transformTitleToPrefix('logs/2024/01/15')).toBe('logs-2024-01-15');
  });

  it('preserves allowed characters (lowercase letters, numbers, underscores, hyphens, plus signs)', () => {
    expect(transformTitleToPrefix('abcdefghijklmnopqrstuvwxyz')).toBe('abcdefghijklmnopqrstuvwxyz');
    expect(transformTitleToPrefix('0123456789')).toBe('0123456789');
    expect(transformTitleToPrefix('my_index_name')).toBe('my_index_name');
    expect(transformTitleToPrefix('my+index+name')).toBe('my+index+name');
    expect(transformTitleToPrefix('my-index-name')).toBe('my-index-name');
  });

  it('converts extended Latin characters to basic Latin letters', () => {
    expect(transformTitleToPrefix('café')).toBe('cafe');
    expect(transformTitleToPrefix('español')).toBe('espanol');
    expect(transformTitleToPrefix('münchen')).toBe('munchen');
    expect(transformTitleToPrefix('français')).toBe('francais');
    expect(transformTitleToPrefix('àèìòù')).toBe('aeiou');
    expect(transformTitleToPrefix('Åse Ørsted')).toBe('ase-orsted');
    expect(transformTitleToPrefix('Índice Principal')).toBe('indice-principal');
  });

  it('trims whitespace', () => {
    expect(transformTitleToPrefix('')).toBe('');
    expect(transformTitleToPrefix('   ')).toBe('');
    expect(transformTitleToPrefix('  my index  ')).toBe('my-index');
  });

  it('collapses multiple consecutive hyphens', () => {
    expect(transformTitleToPrefix('my--index')).toBe('my-index');
    expect(transformTitleToPrefix('my------index')).toBe('my-index');
    expect(transformTitleToPrefix('my!!!index')).toBe('my-index');
  });

  describe('real-world examples', () => {
    it('handles typical index set titles', () => {
      expect(transformTitleToPrefix('Production Logs 2024')).toBe('production-logs-2024');
      expect(transformTitleToPrefix('App Logs (Development)')).toBe('app-logs-development');
      expect(transformTitleToPrefix('logs.application.v1.0')).toBe('logs-application-v1-0');
      expect(transformTitleToPrefix('Système de Logs')).toBe('systeme-de-logs');
      expect(transformTitleToPrefix('My@Special#Index$Name%2024')).toBe('my-special-index-name-2024');
      expect(transformTitleToPrefix('Production 🚀 Logs')).toBe('production-logs');
      expect(transformTitleToPrefix('2024 Production Logs')).toBe('2024-production-logs');
      expect(transformTitleToPrefix('  _-Café & Restaurant Lögs (2024) v1.0!  ')).toBe(
        'cafe-restaurant-logs-2024-v1-0',
      );
    });
  });
});
