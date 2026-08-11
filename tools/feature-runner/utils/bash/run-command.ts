import {execSync} from 'node:child_process';

export const runCommand = (command: string, args?: {errorMessage?: string}) => {
  command = command.trim();
  try {
    return execSync(command).toString().trim();
  } catch (error) {
    throw new Error(args?.errorMessage || `Failed to execute the command: "${command}".`, {
      cause: error,
    });
  }
};
