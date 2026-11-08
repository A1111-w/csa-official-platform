import js from '@eslint/js'
import ts from 'typescript-eslint'
import vue from 'eslint-plugin-vue'

export default ts.config(
  { ignores: ['dist/**', 'node_modules/**', 'test-results/**', 'playwright-report/**', 'coverage/**'] },
  js.configs.recommended,
  ...ts.configs.recommended,
  ...vue.configs['flat/essential'],
  // vue-tsc checks unresolved names, including DOM types, in TypeScript SFC scripts.
  { files: ['**/*.vue'], languageOptions: { parserOptions: { parser: ts.parser } }, rules: { 'no-undef': 'off' } },
  { rules: { 'vue/multi-word-component-names': 'off' } },
)
