import { useCallback, useRef, useState } from 'react'
import { runtime, gatewayHeaders } from '../../../../config/runtime'
import { getErrorMessage } from '../../../../utils/errorMessages'
import { useUser } from '../../../platform/providers/UserContext'
import type {
    ChannelDetail,
    ChannelLoginState,
    ChannelMutationResponse,
    ChannelSelfTestResult,
    ChannelSummary,
    ChannelUpsertRequest,
    ChannelVerificationResult,
} from '../../../../types/channel'

interface UseChannelsResult {
    channels: ChannelSummary[]
    channel: ChannelDetail | null
    isLoading: boolean
    isSaving: boolean
    error: string | null
    fetchChannels: () => Promise<void>
    fetchChannel: (channelId: string) => Promise<void>
    createChannel: (request: ChannelUpsertRequest) => Promise<ChannelMutationResponse>
    updateChannel: (channelId: string, request: ChannelUpsertRequest) => Promise<ChannelMutationResponse>
    deleteChannel: (channelId: string) => Promise<{ success: boolean; error?: string }>
    setChannelEnabled: (channelId: string, enabled: boolean) => Promise<ChannelMutationResponse>
    verifyChannel: (channelId: string) => Promise<{ success: boolean; verification?: ChannelVerificationResult; error?: string }>
    startLogin: (channelId: string) => Promise<{ success: boolean; state?: ChannelLoginState; error?: string }>
    fetchLoginState: (channelId: string) => Promise<{ success: boolean; state?: ChannelLoginState; error?: string }>
    logoutChannel: (channelId: string) => Promise<{ success: boolean; state?: ChannelLoginState; error?: string }>
    runSelfTest: (channelId: string, text: string) => Promise<{ success: boolean; result?: ChannelSelfTestResult; error?: string }>
}

function defaultMutationError(message: string): ChannelMutationResponse {
    return { success: false, error: message }
}

async function channelRequest<T>(
    url: string,
    userId: string | null,
    setError: (err: string | null) => void,
    busyFlag: React.MutableRefObject<boolean>,
    options: {
        method?: string
        body?: unknown
        errorPrefix: string
        isOk?: (data: unknown) => boolean
    },
): Promise<{ data: T | null; error: string | null }> {
    setError(null)
    busyFlag.current = true
    try {
        const init: RequestInit = {
            headers: gatewayHeaders(userId),
            signal: AbortSignal.timeout(10_000),
        }
        if (options.method) init.method = options.method
        if (options.body !== undefined) init.body = JSON.stringify(options.body)

        const response = await fetch(url, init)
        const data = await response.json() as T
        const ok = options.isOk ? options.isOk(data) : response.ok
        if (!ok) {
            const message = (data as { error?: string }).error || options.errorPrefix
            setError(message)
            return { data: null, error: message }
        }
        return { data, error: null }
    } catch (err) {
        const message = getErrorMessage(err)
        setError(message)
        return { data: null, error: message }
    } finally {
        busyFlag.current = false
    }
}

export function useChannels(): UseChannelsResult {
    const { userId } = useUser()
    const [channels, setChannels] = useState<ChannelSummary[]>([])
    const [channel, setChannel] = useState<ChannelDetail | null>(null)
    const [isLoading, setIsLoading] = useState(false)
    const [isSaving, setIsSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const loadingRef = useRef(false)
    const savingRef = useRef(false)

    const base = `${runtime.GATEWAY_URL}/channels`

    const fetchChannels = useCallback(async () => {
        setIsLoading(true)
        const { data, error } = await channelRequest<{ channels?: ChannelSummary[] }>(
            base, userId, setError, loadingRef, { errorPrefix: 'Failed to fetch channels' },
        )
        if (!error && data) setChannels(data.channels ?? [])
        setIsLoading(false)
    }, [userId])

    const fetchChannel = useCallback(async (channelId: string) => {
        setIsLoading(true)
        const { data, error } = await channelRequest<ChannelDetail>(
            `${base}/${channelId}`, userId, setError, loadingRef, { errorPrefix: 'Failed to fetch channel' },
        )
        if (!error && data) setChannel(data)
        setIsLoading(false)
    }, [userId])

    const createChannel = useCallback(async (request: ChannelUpsertRequest): Promise<ChannelMutationResponse> => {
        setIsSaving(true)
        const { data, error } = await channelRequest<ChannelMutationResponse>(
            base, userId, setError, savingRef,
            { method: 'POST', body: request, errorPrefix: 'Failed to create channel', isOk: d => (d as ChannelMutationResponse).success },
        )
        setIsSaving(false)
        if (error) return defaultMutationError(error)
        if (data?.channel) setChannel(data.channel)
        return data!
    }, [userId])

    const updateChannel = useCallback(async (channelId: string, request: ChannelUpsertRequest): Promise<ChannelMutationResponse> => {
        setIsSaving(true)
        const { data, error } = await channelRequest<ChannelMutationResponse>(
            `${base}/${channelId}`, userId, setError, savingRef,
            { method: 'PUT', body: request, errorPrefix: 'Failed to update channel', isOk: d => (d as ChannelMutationResponse).success },
        )
        setIsSaving(false)
        if (error) return defaultMutationError(error)
        if (data?.channel) setChannel(data.channel)
        return data!
    }, [userId])

    const deleteChannel = useCallback(async (channelId: string) => {
        setIsSaving(true)
        const { error } = await channelRequest<{ success: boolean; error?: string }>(
            `${base}/${channelId}`, userId, setError, savingRef,
            { method: 'DELETE', errorPrefix: 'Failed to delete channel', isOk: d => (d as { success: boolean }).success },
        )
        setIsSaving(false)
        if (error) return { success: false, error }
        return { success: true as const }
    }, [userId])

    const setChannelEnabled = useCallback(async (channelId: string, enabled: boolean): Promise<ChannelMutationResponse> => {
        setIsSaving(true)
        const { data, error } = await channelRequest<ChannelMutationResponse>(
            `${base}/${channelId}/${enabled ? 'enable' : 'disable'}`, userId, setError, savingRef,
            { method: 'POST', errorPrefix: 'Failed to update channel status', isOk: d => (d as ChannelMutationResponse).success },
        )
        setIsSaving(false)
        if (error) return defaultMutationError(error)
        if (data?.channel) setChannel(data.channel)
        return data!
    }, [userId])

    const verifyChannel = useCallback(async (channelId: string) => {
        setIsSaving(true)
        const { data, error } = await channelRequest<{ verification?: ChannelVerificationResult; error?: string }>(
            `${base}/${channelId}/verify`, userId, setError, savingRef,
            { method: 'POST', errorPrefix: 'Failed to verify channel' },
        )
        setIsSaving(false)
        if (error) return { success: false as const, error }
        return { success: true as const, verification: data?.verification }
    }, [userId])

    const startLogin = useCallback(async (channelId: string) => {
        setIsSaving(true)
        const { data, error } = await channelRequest<{ state?: ChannelLoginState; error?: string }>(
            `${base}/${channelId}/login`, userId, setError, savingRef,
            { method: 'POST', errorPrefix: 'Failed to start login', isOk: d => (d as { success?: boolean }).success !== false },
        )
        setIsSaving(false)
        if (error) return { success: false as const, error }
        return { success: true as const, state: data?.state }
    }, [userId])

    const fetchLoginState = useCallback(async (channelId: string) => {
        setIsSaving(true)
        const { data, error } = await channelRequest<{ state?: ChannelLoginState; error?: string }>(
            `${base}/${channelId}/login-state`, userId, setError, savingRef,
            { errorPrefix: 'Failed to fetch login state', isOk: d => !!(d as { state?: unknown }).state },
        )
        setIsSaving(false)
        if (error) return { success: false as const, error }
        return { success: true as const, state: data?.state }
    }, [userId])

    const logoutChannel = useCallback(async (channelId: string) => {
        setIsSaving(true)
        const { data, error } = await channelRequest<{ state?: ChannelLoginState; error?: string }>(
            `${base}/${channelId}/logout`, userId, setError, savingRef,
            { method: 'POST', errorPrefix: 'Failed to clear login state', isOk: d => (d as { success?: boolean }).success !== false },
        )
        setIsSaving(false)
        if (error) return { success: false as const, error }
        return { success: true as const, state: data?.state }
    }, [userId])

    const runSelfTest = useCallback(async (channelId: string, text: string) => {
        setIsSaving(true)
        const { data, error } = await channelRequest<{ result?: ChannelSelfTestResult; error?: string }>(
            `${base}/${channelId}/self-test`, userId, setError, savingRef,
            { method: 'POST', body: { text }, errorPrefix: 'Failed to run self-test', isOk: d => (d as { success?: boolean }).success !== false },
        )
        setIsSaving(false)
        if (error) return { success: false as const, error }
        return { success: true as const, result: data?.result }
    }, [userId])

    return {
        channels,
        channel,
        isLoading,
        isSaving,
        error,
        fetchChannels,
        fetchChannel,
        createChannel,
        updateChannel,
        deleteChannel,
        setChannelEnabled,
        verifyChannel,
        startLogin,
        fetchLoginState,
        logoutChannel,
        runSelfTest,
    }
}
