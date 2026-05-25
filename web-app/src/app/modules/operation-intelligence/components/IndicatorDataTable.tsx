import { useState, useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { useUser } from '../../../platform/providers/UserContext'
import { getIndicatorDetail } from '../../../../services/operationIntelligenceAPI'
import Pagination from '../../../platform/ui/primitives/Pagination'

interface Column {
    headerKey: string
    render: (row: Record<string, unknown>, index: number) => React.ReactNode
}

interface IndicatorDataTableProps {
    className: string
    endpoint: string
    envCode: string
    startTime: number
    endTime: number
    rowKey: (row: Record<string, unknown>, index: number) => string | number
    columns: Column[]
}

export default function IndicatorDataTable({
    className, endpoint, envCode, startTime, endTime, rowKey, columns,
}: IndicatorDataTableProps) {
    const { t } = useTranslation()
    const { userId } = useUser()
    const [data, setData] = useState<Record<string, unknown>[]>([])
    const [page, setPage] = useState(1)
    const [total, setTotal] = useState(0)
    const [error, setError] = useState<string | null>(null)
    const tRef = useRef(t)
    tRef.current = t

    useEffect(() => {
        if (!envCode) return
        setError(null)
        getIndicatorDetail(endpoint, envCode, startTime, endTime, page, 10, userId)
            .then((res: { results?: Record<string, unknown>[]; total?: number }) => {
                setData(res.results || [])
                setTotal(res.total || 0)
            })
            .catch((err) => {
                setData([])
                setTotal(0)
                setError(err instanceof Error ? err.message : tRef.current('operationIntelligence.loadFailed'))
            })
    }, [endpoint, envCode, startTime, endTime, page, userId])

    if (error) {
        return (
            <div className="conn-banner conn-banner-error">
                {t('operationIntelligence.loadFailedWithReason', { error })}
            </div>
        )
    }

    if (data.length === 0) {
        return (
            <div className="empty-state">
                <div className="empty-state-title">{t('operationIntelligence.noData')}</div>
            </div>
        )
    }

    return (
        <div className={className}>
            <table>
                <thead>
                    <tr>
                        {columns.map((col, i) => (
                            <th key={i}>{t(`operationIntelligence.${col.headerKey}`)}</th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {data.map((row, i) => (
                        <tr key={rowKey(row, i)}>
                            {columns.map((col, ci) => (
                                <td key={ci}>{col.render(row, i)}</td>
                            ))}
                        </tr>
                    ))}
                </tbody>
            </table>
            {total > 10 && (
                <Pagination
                    currentPage={page}
                    totalPages={Math.ceil(total / 10)}
                    pageSize={10}
                    totalItems={total}
                    onPageChange={setPage}
                />
            )}
        </div>
    )
}
