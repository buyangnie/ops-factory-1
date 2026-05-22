const EMBED_SESSION_KEY = 'ops_embed_mode'

export function getUrlParams(): URLSearchParams {
    const params = new URLSearchParams(window.location.search)
    const hash = window.location.hash
    const queryIndex = hash.indexOf('?')

    if (queryIndex >= 0) {
        const hashParams = new URLSearchParams(hash.slice(queryIndex + 1))
        for (const [key, value] of hashParams.entries()) {
            if (!params.has(key)) {
                params.set(key, value)
            }
        }
    }

    return params
}

export function getUrlParam(name: string): string | null {
    return getUrlParams().get(name)
}

export function isEmbedMode(): boolean {
    const urlValue = getUrlParam('embed')
    if (urlValue === 'true') {
        try { sessionStorage.setItem(EMBED_SESSION_KEY, 'true') } catch { /* storage unavailable */ }
        return true
    }
    if (urlValue === 'false') {
        try { sessionStorage.removeItem(EMBED_SESSION_KEY) } catch { /* storage unavailable */ }
        return false
    }
    try { return sessionStorage.getItem(EMBED_SESSION_KEY) === 'true' } catch { return false }
}
