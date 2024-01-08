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
// eslint-disable-next-line no-unused-vars, no-unused-vars
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import ThreatIntelPluginConfig from './components/ThreatIntelPluginConfig';
import {
  SpamhausEDROPAdapterDocumentation,
  SpamhausEDROPAdapterFieldSet,
  SpamhausEDROPAdapterSummary,
} from './components/adapters/spamhaus-edrop';
import {
  TorExitNodeAdapterDocumentation,
  TorExitNodeAdapterFieldSet,
  TorExitNodeAdapterSummary,
} from './components/adapters/torexitnode';
import {
  WhoisAdapterDocumentation,
  WhoisAdapterFieldSet,
  WhoisAdapterSummary,
} from './components/adapters/whois/index';
import {
  AbuseChRansomAdapterDocumentation,
  AbuseChRansomAdapterFieldSet,
  AbuseChRansomAdapterSummary,
} from './components/adapters/abusech/index';
import { OTXAdapterDocumentation, OTXAdapterFieldSet, OTXAdapterSummary } from './components/adapters/otx';

const bindings = {
  systemConfigurations: [
    {
      component: ThreatIntelPluginConfig,
      displayName: 'Threat Intelligence Lookup',
      configType: 'org.graylog.plugins.threatintel.ThreatIntelPluginConfiguration',
    },
  ],
  lookupTableAdapters: [
    {
      type: 'spamhaus-edrop',
      displayName: 'Spamhaus (E)DROP',
      formComponent: SpamhausEDROPAdapterFieldSet,
      summaryComponent: SpamhausEDROPAdapterSummary,
      documentationComponent: SpamhausEDROPAdapterDocumentation,
    },
    {
      type: 'torexitnode',
      displayName: 'Tor Exit Node',
      formComponent: TorExitNodeAdapterFieldSet,
      summaryComponent: TorExitNodeAdapterSummary,
      documentationComponent: TorExitNodeAdapterDocumentation,
    },
    {
      type: 'whois',
      displayName: 'Whois for IPs',
      formComponent: WhoisAdapterFieldSet,
      summaryComponent: WhoisAdapterSummary,
      documentationComponent: WhoisAdapterDocumentation,
    },
    {
      type: 'abuse-ch-ransom',
      displayName: '[Deprecated] Ransomware blocklists from abuse.ch',
      formComponent: AbuseChRansomAdapterFieldSet,
      summaryComponent: AbuseChRansomAdapterSummary,
      documentationComponent: AbuseChRansomAdapterDocumentation,
    },
    {
      type: 'otx-api',
      displayName: 'Alienvault OTX API',
      formComponent: OTXAdapterFieldSet,
      summaryComponent: OTXAdapterSummary,
      documentationComponent: OTXAdapterDocumentation,
    },
  ],
};

export default bindings;
