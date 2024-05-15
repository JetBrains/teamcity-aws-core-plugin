import { post } from './fetchHelper';
import {
  getErrorsFromResponseIfAny,
  parseResponse,
} from './responseParserUtils';

const url = '/repo/aws-test-connection.html';
const BASE_TEST_CONNECTION_PREFIX = 'Running STS get-caller-identity...\n';

type TestAwsConnectionResponse = {
  success: boolean;
  message: string;
};
export async function testAwsConnection(formData: {
  [key: string]: string | null;
}): Promise<TestAwsConnectionResponse> {
  const responseStr = await post(url, formData);
  const response = new DOMParser().parseFromString(responseStr, 'text/xml');
  const errors = getErrorsFromResponseIfAny(response);
  const result: Element | undefined = parseResponse(
    response,
    'callerIdentity'
  )[0];
  if (result) {
    const account = result.getAttribute('accountId');
    const userId = result.getAttribute('userId');
    const arn = result.getAttribute('userArn');

    return {
      success: true,
      message: `${BASE_TEST_CONNECTION_PREFIX}Caller Identity:\n Account ID: ${account}\n User ID: ${userId}\n ARN: ${arn}`,
    };
  } else if (errors) {
    const message = Object.keys(errors)
      .map((key) => errors[key].message)
      .join('\n');
    return {
      success: false,
      message,
    };
  } else {
    return {
      success: true,
      message: `${BASE_TEST_CONNECTION_PREFIX}Could not get the Caller Identity information from the response.`,
    };
  }
}
