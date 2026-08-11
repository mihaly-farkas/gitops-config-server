import {When} from '@cucumber/cucumber';
import {runCommand} from '../utils/bash/run-command';

When('I run the following command:', (bashCommand: string) => runCommand(bashCommand));
