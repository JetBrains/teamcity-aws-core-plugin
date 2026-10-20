import { post } from './fetchHelper';

export default async function getGeneratedId(
  displayName: string,
  projectId: string
) {
  return await post('generateId.html', {
    object: 'awsConnection',
    name: displayName,
    parentId: projectId,
  });
}
