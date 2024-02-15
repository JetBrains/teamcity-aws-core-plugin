import { React } from '@jetbrains/teamcity-api';

export function defaultArraySplitter(
  serializedArray: string | null | undefined
): string[] {
  if (serializedArray) {
    return serializedArray
      .replace(/[\[\]]/g, '')
      .split(/,/)
      .map((it) => it.trim().replace(/#/g, ','));
  } else {
    return [];
  }
}

export function defaultArrayJoiner(arr: string[] | null | undefined) {
  if (!arr || !arr.length) {
    return null;
  }
  return arr.map((s) => s.trim().replace(/,/g, '#')).join(',');
}

export default function useDefaultSplitter(serializedArray: string) {
  return React.useMemo(
    () => defaultArraySplitter(serializedArray),
    [serializedArray]
  );
}
