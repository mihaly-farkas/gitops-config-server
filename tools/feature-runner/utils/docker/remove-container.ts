import {runCommand} from '../bash/run-command';

export const removeContainer = (containerName: string) => {
  const commandResult = runCommand(`docker ps -a --filter "name=${containerName}" --format '{{.Names}}'`);

  if (commandResult === containerName) {
    runCommand(`docker rm ${containerName}`);
  }
};
