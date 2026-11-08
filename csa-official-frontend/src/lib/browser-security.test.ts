// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AxiosHeaders, type AxiosAdapter } from 'axios'
import api, { clearCsrfToken } from '@/lib/axios'
import { sanitizeHtml } from '@/lib/sanitize-html'
import { getSafeRedirect } from '@/lib/navigation'
afterEach(() => { clearCsrfToken(); vi.unstubAllGlobals() })
describe('browser security compatibility', () => {
  it('shares the CSRF request and sends its token with cookie-based writes', async () => {
    const fetcher = vi.fn().mockResolvedValue({ ok: true, json: async () => ({ code: 200, data: { csrfToken: 'fixture-csrf' } }) })
    vi.stubGlobal('fetch', fetcher)
    const adapter: AxiosAdapter = async config => {
      expect(config.withCredentials).toBe(true)
      expect(config.headers.get('X-CSRF-Token')).toBe('fixture-csrf')
      expect(config.headers.has('Authorization')).toBe(false)
      return { config, headers: new AxiosHeaders(), status: 200, statusText: 'OK', data: { code: 200, data: 'saved' } }
    }
    await Promise.all([api.post('/api/ai/documents', {}, { adapter }), api.delete('/api/ai/documents/test', { adapter })])
    expect(fetcher).toHaveBeenCalledTimes(1)
    expect(fetcher).toHaveBeenCalledWith('/api/auth/csrf', { credentials: 'include' })
  })
  it('removes active HTML and unsafe links while retaining rich text', () => {
    const html = sanitizeHtml('<h2>简介</h2><script>alert(1)</script><a href="javascript:alert(1)" onclick="alert(1)">link</a><iframe src="https://x.test"></iframe>')
    expect(html).toContain('<h2>简介</h2>')
    expect(html).not.toMatch(/script|onclick|iframe|javascript:/)
  })
  it('rejects cross-origin and looping login redirects', () => {
    for (const value of ['https://evil.test', '//evil.test', '/\\evil.test', '/login?redirect=/dashboard']) {
      expect(getSafeRedirect(value)).toBe('/dashboard')
    }
    expect(getSafeRedirect('/dashboard/ai?tab=knowledge')).toBe('/dashboard/ai?tab=knowledge')
  })
})
