import { ResponseErrors } from '@jetbrains-internal/tcci-react-ui-components';

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
