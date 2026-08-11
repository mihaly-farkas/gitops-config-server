import {Given, setWorldConstructor, Then, World} from '@cucumber/cucumber';
import {getReasonPhrase, StatusCodes} from 'http-status-codes';
import axios, {AxiosInstance, AxiosRequestConfig, AxiosResponse} from 'axios';
import {expect} from 'chai';
import {parse as parseYaml} from 'yaml';
import * as http from 'node:http';
import * as https from 'node:https';
import * as cheerio from 'cheerio';

export class HttpRequestWorld extends World {
  config: AxiosRequestConfig = {};

  private _client?: AxiosInstance;
  private _httpAgent?: http.Agent;
  private _httpsAgent?: https.Agent;
  private _response?: AxiosResponse;

  get response() {
    if (this._response === undefined) {
      throw new Error('Response is not available. Please call the request() method first.');
    }
    return this._response;
  }

  private _json?: unknown;

  get json() {
    if (this._json === undefined) {
      throw new Error(
        'JSON is not available.' +
          " Please use the 'the body is a JSON object' step first to parse the response body as JSON.",
      );
    }
    return this._json;
  }

  set json(value: unknown) {
    this._json = value;
  }

  private _yaml?: unknown;

  get yaml() {
    if (this._yaml === undefined) {
      throw new Error(
        'YAML is not available.' +
          " Please use the 'the body is a YAML document' step first to parse the response body as YAML.",
      );
    }
    return this._yaml;
  }

  set yaml(value: unknown) {
    this._yaml = value;
  }

  private _html?: string;

  get html() {
    if (this._html === undefined) {
      throw new Error(
        'HTML is not available.' +
          " Please use the 'the response is an HTML document' step first to parse the response body as HTML.",
      );
    }
    return this._html;
  }

  set html(value: string) {
    this._html = value;
  }

  get data() {
    if (this._response === undefined) {
      throw new Error('Response is not available. Please call the request() method first.');
    }
    let data;
    if (this._json !== undefined) {
      data = this._json;
    } else if (this._yaml !== undefined) {
      data = this._yaml;
    } else {
      throw new Error(
        'Data is not available.' +
          " Please use the 'the body is a JSON object' or 'the body is a YAML document'" +
          ' step first to parse the response body.',
      );
    }
    return data;
  }

  async request() {
    if (this._response === undefined) {
      this._httpAgent = new http.Agent({keepAlive: true});
      this._httpsAgent = new https.Agent({keepAlive: true});
      this._client = axios.create({
        httpAgent: this._httpAgent,
        httpsAgent: this._httpsAgent,
        timeout: 10000,
      });
      this._response = await this._client.request(this.config);
    }
    return this;
  }

  reset() {
    this._httpAgent?.destroy();
    this._httpsAgent?.destroy();

    this.config = {};
    this._client = undefined;
    this._httpAgent = undefined;
    this._httpsAgent = undefined;
    this._response = undefined;
    this._json = undefined;
    this._yaml = undefined;
  }
}

setWorldConstructor(HttpRequestWorld);

Given('I send a GET request to the {string} URL', function (this: HttpRequestWorld, url: string) {
  this.reset();
  this.config.method = 'GET';
  this.config.url = url;
});

Given(
  'with basic HTTP authentication using the {string} username and the {string} password',
  async function (this: HttpRequestWorld, username: string, password: string) {
    this.config.auth = {username, password};
  },
);

Then(
  'the response status is {string}',
  async function (this: HttpRequestWorld, expectedStatusCodeAndStatusText: string) {
    // Validate the expected status code and status text format
    expect(expectedStatusCodeAndStatusText).to.match(
      /^\d{3} .+$/,
      `Invalid step definition 'the response status is "${expectedStatusCodeAndStatusText}"'.` +
        ' The provided status code and status text does not match the expected format "{status-code} {status-text}"',
    );

    // Split the input into status code and status text
    const [statusCodeStr] = expectedStatusCodeAndStatusText.split(' ');
    const statusCode = Number.parseInt(statusCodeStr, 10);

    // Validate if the status code is a valid HTTP status code
    expect(Object.values(StatusCodes)).to.include(
      statusCode,
      `Invalid step definition 'the response status is "${expectedStatusCodeAndStatusText}"'.` +
        ` The provided status code "${statusCode}" is not a valid HTTP status code`,
    );

    // Validate if the status text matches the expected status text for the given status code
    const validStatusCodeAndStatusText = statusCode + ' ' + getReasonPhrase(statusCode);
    expect(validStatusCodeAndStatusText).to.equal(
      expectedStatusCodeAndStatusText,
      `Invalid step definition 'the response status is "${expectedStatusCodeAndStatusText}"'`,
    );

    await this.request();
    expect(this.response.status).to.equal(statusCode);
  },
);

Then('requires HTTP basic auth', async function (this: HttpRequestWorld) {
  await this.request();
  expect(this.response.status).to.equal(401);
  expect(this.response.headers['www-authenticate']).to.include('Basic realm="Realm", charset="UTF-8"');
});

Then('the response is a JSON object', async function (this: HttpRequestWorld) {
  await this.request();
  expect(this.response.headers['content-type']).to.include('application/json');
  expect(this.response.data).to.be.an('object');
  this.json = this.response.data;
});

Then('the response is an HTML document', async function (this: HttpRequestWorld) {
  await this.request();
  expect(this.response.headers['content-type']).to.include('text/html;charset=UTF-8');
  expect(this.response.data).to.be.a('string');
  expect(this.response.data).to.include('<!DOCTYPE html>');
  expect(this.response.data).to.include('<html');
  this.html = this.response.data;
});

Then('the response content type is {string}', async function (this: HttpRequestWorld, expectedContentType: string) {
  await this.request();
  expect(this.response.headers['content-type']).to.include(expectedContentType);
});

Then('the response body is a JSON object', async function (this: HttpRequestWorld) {
  await this.request();
  expect(this.response.data).to.be.an('object');
  this.json = this.response.data;
});

Then('the response is a YAML document', async function (this: HttpRequestWorld) {
  await this.request();
  expect(this.response.headers['content-type']).to.include('text/plain');
  expect(this.response.data).to.be.a('string');
  let yaml;
  expect(() => (yaml = parseYaml(this.response.data))).not.to.throw();
  this.yaml = yaml;
});

Then('the body is a YAML document', async function (this: HttpRequestWorld) {
  await this.request();
  expect(this.response.data).to.be.a('string');
  let yaml;
  expect(() => (yaml = parseYaml(this.response.data))).not.to.throw();
  this.yaml = yaml;
});

Then(
  'the {string} field is equal to {string}',
  async function (this: HttpRequestWorld, fieldSelector: string, expectedValue: string) {
    await this.request();

    const actualValue = fieldSelector.split('.').reduce<unknown>((obj, key) => {
      if (obj !== null && typeof obj === 'object') {
        return (obj as Record<string, unknown>)[key];
      }
      return undefined;
    }, this.data);

    expect(actualValue).to.equal(
      expectedValue,
      'The value of the field "' + fieldSelector + '" is not equal to the expected value',
    );
  },
);

Then(
  'the {string} field is greater than {string}',
  async function (this: HttpRequestWorld, fieldSelector: string, expectedGreaterThanValueString: string) {
    expect(expectedGreaterThanValueString).to.match(
      /^\d+$/,
      `Invalid step definition 'the ${fieldSelector} field is greater than ${expectedGreaterThanValueString}'.` +
        ` The provided expected value "${expectedGreaterThanValueString}" is not a valid number`,
    );
    const expectedGreaterThanValue = Number(expectedGreaterThanValueString);

    await this.request();

    const actualValue = fieldSelector.split('.').reduce<unknown>((obj, key) => {
      if (obj !== null && typeof obj === 'object') {
        return (obj as Record<string, unknown>)[key];
      }
      return undefined;
    }, this.data);

    expect(actualValue).to.be.greaterThan(
      expectedGreaterThanValue,
      'The value of the field "' + fieldSelector + '" is not greater than the expected value',
    );
  },
);

Then('the {string} field is undefined', async function (this: HttpRequestWorld, fieldSelector: string) {
  await this.request();

  const actualValue = fieldSelector.split('.').reduce<unknown>((obj, key) => {
    if (obj !== null && typeof obj === 'object') {
      return (obj as Record<string, unknown>)[key];
    }
    return undefined;
  }, this.data);

  expect(actualValue).to.be.undefined;
});

Then(
  'the HTML {string} is {string}',
  async function (this: HttpRequestWorld, cheerioSelector: string, expectedValueString: string) {
    await this.request();

    const $ = cheerio.load(this.response.data);

    expect($(cheerioSelector).text()).to.equal(expectedValueString);
  },
);
