import { useMemo } from 'react'
import IndicatorDataTable from './IndicatorDataTable'

interface AlarmDetailTableProps {
    envCode: string
    startTime: number
    endTime: number
}

const td = (row: Record<string, unknown>, key: string) => String(row[key] ?? '')

export default function AlarmDetailTable({ envCode, startTime, endTime }: AlarmDetailTableProps) {
    const columns = useMemo(() => [
        { headerKey: 'timestamp', render: (row: Record<string, unknown>) => row.occurUtc ? new Date(Number(row.occurUtc)).toLocaleString() : '' },
        { headerKey: 'alarmName', render: (row: Record<string, unknown>) => td(row, 'alarmName') },
        { headerKey: 'severity', render: (row: Record<string, unknown>) => td(row, 'severity') },
        { headerKey: 'dn', render: (row: Record<string, unknown>) => td(row, 'dn') },
        { headerKey: 'count', render: (row: Record<string, unknown>) => td(row, 'count') },
        { headerKey: 'alarmDesc', render: (row: Record<string, unknown>) => td(row, 'moi') },
        { headerKey: 'alarmDetail', render: (row: Record<string, unknown>) => td(row, 'additionalInformation') },
    ], [])

    return (
        <IndicatorDataTable
            className="alarm-detail-table"
            endpoint="/qos/getAlarmIndicatorDetail"
            envCode={envCode}
            startTime={startTime}
            endTime={endTime}
            rowKey={(row, i) => row.alarmName ? `${row.alarmName}-${row.occurUtc}-${i}` : i}
            columns={columns}
        />
    )
}
