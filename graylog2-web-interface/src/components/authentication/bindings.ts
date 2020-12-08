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
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import ConfigParser from 'logic/authentication/directoryServices/BackendConfigParser';

import BackendCreateLDAP from './directoryServices/ldap/BackendCreate';
import BackendEditLDAP from './directoryServices/ldap/BackendEdit';
import BackendConfigDetailsAD from './directoryServices/activeDirectory/BackendConfigDetails';
import BackendConfigDetailsLDAP from './directoryServices/ldap/BackendConfigDetails';
import BackendCreateAD from './directoryServices/activeDirectory/BackendCreate';
import BackendEditAD from './directoryServices/activeDirectory/BackendEdit';

PluginStore.register(new PluginManifest({}, {
  'authentication.services': [
    {
      name: 'ldap',
      displayName: 'LDAP',
      createComponent: BackendCreateLDAP,
      editComponent: BackendEditLDAP,
      configDetailsComponent: BackendConfigDetailsLDAP,
      configToJson: ConfigParser.toJson,
      configFromJson: ConfigParser.fromJson,
    },
    {
      name: 'active-directory',
      displayName: 'Active Directory',
      createComponent: BackendCreateAD,
      editComponent: BackendEditAD,
      configDetailsComponent: BackendConfigDetailsAD,
      configToJson: ConfigParser.toJson,
      configFromJson: ConfigParser.fromJson,
    },
  ],
}));
