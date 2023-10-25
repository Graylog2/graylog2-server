export type ContentPackInstallation = {
  created_at: string,
  description: string,
  entities: Array<ContentPackEntity>,
  id: string,
  name: string,
  parameters: Array<any>,
  rev: number,
  server_version: string,
  summary: string,
  url: string,
  v: number,
  vendor: string,
}

export type ContentPackEntity = {
  id: string,
  type: EntityType,
  v: string,
  data: Data,
  constraints: Array<Constraint>,
}

export interface EntityType {
  name: string
  version: string
}

export interface Data {
  [key: string]: Type | { [key: string]: Type },
}

export interface Type {
  '@type': string,
  '@value': string,
}

export interface Constraint {
  type: string,
  plugin?: string,
  version: string,
}

export type ContentPackMetadata = {
  [key: number]: {
    [key: number]: {
      [key: string]: number,
    },
  },
}
