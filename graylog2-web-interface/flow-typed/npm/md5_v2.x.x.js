// flow-typed signature: a8c17ac8b29005199bba525549f2d49a
// flow-typed version: 3a188dce72/md5_v2.x.x/flow_>=v0.25.x

// @flow

declare module "md5" {
  declare module.exports: (
    message: string | Buffer,
    options?: {
      asString?: boolean,
      asBytes?: boolean,
      encoding?: string
    }
  ) => string;
}
