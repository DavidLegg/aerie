export default {
  transform: {
    '^.+\\.tsx?$': 'ts-jest',
  },
  testRunner: 'jest-circus/runner',
  // testRegex: "(/test/.*|(\\.|/)(test|spec))\\.(jsx?|tsx?)$",
  testRegex: '((\\.|/)(test|spec))\\.(jsx?|tsx?)$',
  testPathIgnorePatterns: ['/node_modules/', '/build/'],
  coverageReporters: ['html'],
  setupFiles: ['dotenv/config'],
  globalSetup: "./jestGlobalSetup.js",
  setupFilesAfterEnv: ["jest-extended/all"],
  reporters: [
    'default',
    ['jest-html-reporters', {
      publicPath: '<rootDir>',
      pageTitle: 'command-expansion-server Test Report',
      filename: 'test-report.html',
      inlineSource: true
    }],
  ],
  transformIgnorePatterns: ['[/\\\\]node_modules[/\\\\].+\\.(js|jsx)$'],
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json', 'node'],
  collectCoverageFrom: ['src/**/*.ts'],
  coveragePathIgnorePatterns: [],
  extensionsToTreatAsEsm: ['.ts'],
  moduleNameMapper: {
    '^(\\.{1,2}/.*)\\.js$': '$1',
  },
  globals: {
    'ts-jest': {
      diagnostics: {
        ignoreCodes: []
      },
      useESM: true,
    }
  }
};
