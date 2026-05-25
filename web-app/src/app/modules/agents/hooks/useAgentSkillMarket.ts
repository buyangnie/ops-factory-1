import { useCallback, useState } from 'react'
import type { SkillMarketEntry } from '../../../../types/skillMarket'
import { getErrorMessage } from '../../../../utils/errorMessages'
import { useUser } from '../../../platform/providers/UserContext'
import { fetchSkillList, installSkillToAgent } from '../../../../services/skillMarketAPI'

interface UseAgentSkillMarketResult {
    skills: SkillMarketEntry[]
    isLoading: boolean
    error: string | null
    fetchSkills: () => Promise<void>
    installSkill: (agentId: string, skillId: string) => Promise<{ success: boolean; conflict?: boolean; error?: string }>
}

export function useAgentSkillMarket(): UseAgentSkillMarketResult {
    const { userId } = useUser()
    const [skills, setSkills] = useState<SkillMarketEntry[]>([])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const fetchSkills = useCallback(async () => {
        setIsLoading(true)
        setError(null)
        try {
            setSkills(await fetchSkillList())
        } catch (err) {
            setError(getErrorMessage(err))
        } finally {
            setIsLoading(false)
        }
    }, [])

    const installSkill = useCallback(async (agentId: string, skillId: string) => {
        return installSkillToAgent(agentId, skillId, userId!)
    }, [userId])

    return { skills, isLoading, error, fetchSkills, installSkill }
}
