import { ResponseErrors } from '@jetbrains-internal/tcci-react-ui-components';
import {Config} from "../types";
import {utils} from "@jetbrains/teamcity-api";
import json5 from 'json5';

const regionsRegex = /const allRegions = (\{.*?});/s;
const configRegex = /const config = (\{.*?});/s;


export function getErrorsFromResponseIfAny(response: Document) {
  const errors = response.querySelectorAll('errors > error');
  if (!errors.length) {
    return null;
  } else {
    const result: ResponseErrors = {};
    errors.forEach(
      (elem) => (result[elem.id] = { message: elem.textContent! })
    );
    return result;
  }
}

export function parseResponse(response: Document, selector: string) {
  const result: Element[] = [];
  response.querySelectorAll(selector).forEach((elem) => result.push(elem));

  return result;
}

export async function getConfigForConnection(projectId: string, currentConnection: string): Promise<Config> {
  const loadHtmlContent = async () => {
    const url = `/admin/oauth/showConnection.html?providerType=AWS&projectId=${projectId}&connectionId=${currentConnection}`;

    return await utils.requestText(
        url,
        {
          method: 'POST',
        },
        true
    );
  };

  const content = await loadHtmlContent();
  const regions = parse(content, regionsRegex);
  const config = parse(content, configRegex) as Config;

  if (config) {
    config.allRegions = regions;
  }

  return config;
}

function parse(content: string, regex: RegExp): any {
  try {
    const match = content.match(regex);

    if (match) {
      let preParsed = match[1];
      preParsed = preParsed.replace(/"false" === "true"/g, "false");
      preParsed = preParsed.replace(/"true" === "true"/g, "true");
      preParsed = preParsed.replace(/allRegions: allRegions,/g, '');
      return json5.parse(preParsed);
    }
  } catch (e) {
    console.error('Failed to parse JSON:', e);
  }

  return null;
}
