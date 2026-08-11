import {runCommand} from '../bash/run-command';

export const isContainerExists = (containerName: string): boolean =>
  runCommand(`docker ps -a --filter "name=${containerName}" --format '{{.Names}}'`) === containerName;
