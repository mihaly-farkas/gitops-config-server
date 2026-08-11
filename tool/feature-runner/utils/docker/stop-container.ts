import {runCommand} from '../bash/run-command';

export const stopContainer = (containerName: string) => {
  const commandResult = runCommand(`docker ps --filter "name=${containerName}" --format '{{.Names}}'`);

  if (commandResult === containerName) {
    runCommand(`docker stop ${containerName}`);
  }
};
