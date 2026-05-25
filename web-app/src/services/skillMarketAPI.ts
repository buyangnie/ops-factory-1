import { runtime, gatewayHeaders } from '../config/runtime'
import { getErrorMessage } from '../utils/errorMessages'
import type { SkillMarketDetail, SkillMarketEntry, SkillMarketListResponse, SkillMarketMutationResponse } from '../types/skillMarket'

export async function fetchSkillList(query?: string): Promise<SkillMarketEntry[]> {
    const params = query?.trim() ? `?q=${encodeURIComponent(query.trim())}` : ''
    const response = await fetch(`${runtime.SKILL_MARKET_SERVICE_URL}/skills${params}`, {
        signal: AbortSignal.timeout(10000),
    })
    if (!response.ok) throw new Error(await response.text())
    const data = await response.json() as SkillMarketListResponse
    return data.items || []
}

export async function fetchSkillDetail(skillId: string): Promise<SkillMarketDetail> {
    const response = await fetch(`${runtime.SKILL_MARKET_SERVICE_URL}/skills/${encodeURIComponent(skillId)}`, {
        signal: AbortSignal.timeout(10000),
    })
    if (!response.ok) throw new Error(await response.text())
    return response.json() as Promise<SkillMarketDetail>
}

export async function createSkill(payload: { id: string; name: string; description: string; instructions: string }): Promise<SkillMarketMutationResponse> {
    const response = await fetch(`${runtime.SKILL_MARKET_SERVICE_URL}/skills`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    })
    if (!response.ok) throw new Error(await response.text())
    return response.json() as Promise<SkillMarketMutationResponse>
}

export async function updateSkill(skillId: string, payload: { name: string; description: string; instructions: string }): Promise<SkillMarketMutationResponse> {
    const response = await fetch(`${runtime.SKILL_MARKET_SERVICE_URL}/skills/${encodeURIComponent(skillId)}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    })
    if (!response.ok) throw new Error(await response.text())
    return response.json() as Promise<SkillMarketMutationResponse>
}

export async function deleteSkill(skillId: string): Promise<void> {
    const response = await fetch(`${runtime.SKILL_MARKET_SERVICE_URL}/skills/${encodeURIComponent(skillId)}`, {
        method: 'DELETE',
    })
    if (!response.ok) throw new Error(await response.text())
}

export async function importSkill(file: File, id?: string): Promise<SkillMarketMutationResponse> {
    const formData = new FormData()
    formData.append('file', file)
    if (id?.trim()) formData.append('id', id.trim())
    const response = await fetch(`${runtime.SKILL_MARKET_SERVICE_URL}/skills:import`, {
        method: 'POST',
        body: formData,
    })
    if (!response.ok) {
        const text = await response.text()
        let message = text
        try {
            const body = JSON.parse(text) as { code?: string; message?: string; error?: string }
            message = body.message || body.error || text
            if (body.code === 'SKILL_ALREADY_EXISTS') message = 'SKILL_ALREADY_EXISTS'
        } catch {
            // Keep raw response text.
        }
        throw new Error(message)
    }
    return response.json() as Promise<SkillMarketMutationResponse>
}

export async function installSkillToAgent(
    agentId: string,
    skillId: string,
    userId: string,
): Promise<{ success: boolean; conflict?: boolean; error?: string }> {
    try {
        const response = await fetch(`${runtime.GATEWAY_URL}/agents/${encodeURIComponent(agentId)}/skills/install`, {
            method: 'POST',
            headers: gatewayHeaders(userId),
            body: JSON.stringify({ skillId }),
        })
        if (response.status === 409) {
            return { success: false, conflict: true, error: await response.text() }
        }
        if (!response.ok) throw new Error(await response.text())
        return { success: true }
    } catch (err) {
        return { success: false, error: getErrorMessage(err) }
    }
}
