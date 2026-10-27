// @ts-check
const eslint = require('@eslint/js');
const { defineConfig } = require('eslint/config');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');
const boundaries = require('eslint-plugin-boundaries');

/**
 * Architecture layers (see src/app):
 *
 *   app (bootstrap, routes, config)  → shell, shared, domain root (its *.routes.ts)
 *   shell (layout, header, menu)     → shared, domain services/models
 *   domains/<name>                   → its own files + shared, never another domain
 *     root (*.routes.ts) → pages → components → services → models
 *   shared                           → shared only
 *
 * Element patterns are folders, matched from the project root; the first match wins,
 * so the most specific folders come first.
 */
const SAME_DOMAIN = { domain: '{{ from.element.captured.domain }}' };
const sameDomain = (...types) => types.map((type) => ({ element: { type, captured: SAME_DOMAIN } }));
const layer = (...types) => types.map((type) => ({ element: { type } }));

const architecture = {
  plugins: { boundaries },
  settings: {
    'import/resolver': { typescript: { alwaysTryTypes: true, project: './tsconfig.json' } },
    'boundaries/elements': [
      { type: 'domain-pages', partialMatch: false, pattern: 'src/app/domains/*/pages', capture: ['domain'] },
      { type: 'domain-components', partialMatch: false, pattern: 'src/app/domains/*/components', capture: ['domain'] },
      { type: 'domain-services', partialMatch: false, pattern: 'src/app/domains/*/services', capture: ['domain'] },
      { type: 'domain-models', partialMatch: false, pattern: 'src/app/domains/*/models', capture: ['domain'] },
      { type: 'domain-root', partialMatch: false, pattern: 'src/app/domains/*', capture: ['domain'] },
      { type: 'shell', partialMatch: false, pattern: 'src/app/shell' },
      { type: 'shared', partialMatch: false, pattern: 'src/app/shared' },
      { type: 'app', partialMatch: false, pattern: 'src/app' },
    ],
    // main.ts sits outside src/app: known, but not a layer.
    'boundaries/files': [{ category: 'entry', pattern: 'src/main.ts' }],
  },
  rules: {
    'boundaries/dependencies': [
      'error',
      {
        default: 'disallow',
        policies: [
          // Everyone may use shared.
          { allow: { to: layer('shared') } },
          { from: layer('app'), allow: { to: layer('app', 'shell', 'domain-root') } },
          { from: { file: { categories: 'entry' } }, allow: { to: layer('app') } },
          { from: layer('shell'), allow: { to: layer('shell', 'domain-services', 'domain-models') } },
          // Inside a domain, each layer only reaches down, and only within the same domain.
          { from: layer('domain-root'), allow: { to: sameDomain('domain-root', 'domain-pages') } },
          {
            from: layer('domain-pages'),
            allow: { to: sameDomain('domain-pages', 'domain-components', 'domain-services', 'domain-models') },
          },
          {
            from: layer('domain-components'),
            allow: { to: sameDomain('domain-components', 'domain-services', 'domain-models') },
          },
          { from: layer('domain-services'), allow: { to: sameDomain('domain-services', 'domain-models') } },
          { from: layer('domain-models'), allow: { to: sameDomain('domain-models') } },
        ],
      },
    ],
    // Every file under src/app must belong to a layer, and imports must resolve to one.
    'boundaries/no-unknown-files': 'error',
    'boundaries/no-unknown-dependencies': 'error',
    // Crossing into another layer goes through an alias, so boundaries stay visible in imports.
    'no-restricted-imports': [
      'error',
      {
        patterns: [
          {
            regex: '^(\\.{1,2}/)+(shared|shell|domains)/',
            message: 'Import other layers with @shared/..., @shell/... or @domains/... instead of a relative path.',
          },
        ],
      },
    ],
  },
};

module.exports = defineConfig([
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
    ],
    languageOptions: {
      parserOptions: { projectService: true, tsconfigRootDir: __dirname },
    },
    processor: angular.processInlineTemplates,
    rules: {
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: 'app',
          style: 'camelCase',
        },
      ],
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: 'app',
          style: 'kebab-case',
        },
      ],
      // Signals everywhere, and components only re-render when their signals or inputs change.
      '@angular-eslint/prefer-signals': 'error',
      '@angular-eslint/prefer-on-push-component-change-detection': 'error',
      '@angular-eslint/prefer-standalone': 'error',
      '@angular-eslint/no-async-lifecycle-method': 'error',
      // Promises must be awaited, returned or explicitly ignored with `void`.
      '@typescript-eslint/no-floating-promises': 'error',
      '@typescript-eslint/no-misused-promises': 'error',
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
      eqeqeq: ['error', 'always'],
      'no-console': ['error', { allow: ['error'] }],
    },
  },
  {
    files: ['src/**/*.ts'],
    ...architecture,
  },
  {
    // Only domain services (and shared HTTP plumbing) talk to the backend.
    files: [
      'src/app/shell/**/*.ts',
      'src/app/domains/*/pages/**/*.ts',
      'src/app/domains/*/components/**/*.ts',
      'src/app/domains/*/models/**/*.ts',
    ],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          paths: [{ name: '@angular/common/http', message: 'Call the backend from the domain services layer.' }],
          patterns: [
            {
              regex: '^(\\.{1,2}/)+(shared|shell|domains)/',
              message: 'Import other layers with @shared/..., @shell/... or @domains/... instead of a relative path.',
            },
          ],
        },
      ],
    },
  },
  {
    // Models are plain types: no framework, UI or RxJS code.
    files: ['src/app/domains/*/models/**/*.ts'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            { regex: '^(@angular|primeng|rxjs|@jsverse)(/|$)', message: 'Models are plain TypeScript types.' },
          ],
        },
      ],
    },
  },
  {
    files: ['**/*.spec.ts'],
    rules: {
      // Tests may reach into any layer.
      'boundaries/dependencies': 'off',
    },
  },
  {
    files: ['**/*.html'],
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility],
    rules: {
      '@angular-eslint/template/prefer-control-flow': 'error',
      '@angular-eslint/template/prefer-self-closing-tags': 'error',
      '@angular-eslint/template/no-any': 'error',
    },
  },
]);
