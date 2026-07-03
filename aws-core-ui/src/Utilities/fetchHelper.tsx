import { utils } from '@jetbrains/teamcity-api';

const request = async (
  url: string,
  params: { [k: string]: string | null | undefined } | null,
  method = 'GET'
) => {
  let body: FormData | undefined;
  if (params) {
    body = new FormData();
    Object.keys(params).forEach((key) => {
      const value = params[key] ?? '';
      body!.append(key, value);
    });
  }

  return await utils.requestText(
    url.replace(/^\/+/, ''),
    {
      method,
      body,
    },
    true
  );
};

export const post = async (
  url: string,
  params: { [k: string]: string | null | undefined } | null = null
) => await request(url, params, 'POST');

export const get = async (url: string) => await request(url, null, 'GET');
