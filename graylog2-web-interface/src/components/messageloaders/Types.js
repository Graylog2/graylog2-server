// @flow strict
export type Input = {
  id: string,
  title: string,
  name: string,
  type: string,
  configuration: {
    [type: string]: any,
  },
  input_profile_id: string,
  version: number,
  created_at: string,
  content_pack?: boolean;
};

export type Codec ={
  type: string,
  name: string,
  requested_configuration: {
    [key: string]: {
      [key: string]: any,
    },
  },
};

export type CodecTypes = {
  [key: string]: Codec,
};
