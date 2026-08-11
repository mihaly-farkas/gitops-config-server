import {runCommand} from '../bash/run-command';

export const verifyDocker = (): void => {
  const command = process.platform === 'win32' ? 'docker info' : 'docker ps';
  runCommand(command, {
    errorMessage:
      'Docker is not installed or not running. ' + 'Please ensure Docker is installed and running on your machine.',
  });
};
