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

export type EvidenceTypes = 'logs' | 'dashboards' | 'searches' | 'events';

export type AddEvidenceProps = {
  index?: string,
  id: string,
  type: EvidenceTypes,
  children: React.ReactElement,
};

export type InvestigationsPlugin = {
  components: {
    AddEvidence: React.ComponentType<AddEvidenceProps>,
  },
  hooks: {
    useInvestigationDrawer: () => ({
      selectedInvestigationId: string,
    }),
  }
};

declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    investigationsPlugin?: Array<InvestigationsPlugin>,
  }
}
