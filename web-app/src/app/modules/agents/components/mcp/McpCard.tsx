import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import type { McpEntry } from '../../../../../types/mcp'
import { getMcpDisplayName } from '../../../../../types/mcp'

function EditIcon() {
  return (
    <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <path
        d="M4.75 13.95 4 16l2.05-.75 8.5-8.5-1.3-1.3-8.5 8.5Z"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="m11.95 6.05 1.3 1.3m.65-.65 1.05-1.05a1.15 1.15 0 0 0 0-1.6l-.5-.5a1.15 1.15 0 0 0-1.6 0L11.8 4.6"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M4 16h12"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
      />
    </svg>
  )
}

function ConfigureIcon() {
  return (
    <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <path
        d="M10 3.3 11.1 4l1.3-.22.86 1.01 1.3.23.23 1.3 1.01.86-.22 1.3.7 1.1-.7 1.1.22 1.3-1.01.86-.23 1.3-1.3.23-.86 1.01-1.3-.22-1.1.7-1.1-.7-1.3.22-.86-1.01-1.3-.23-.23-1.3-1.01-.86.22-1.3-.7-1.1.7-1.1-.22-1.3 1.01-.86.23-1.3 1.3-.23.86-1.01 1.3.22 1.1-.7Z"
        stroke="currentColor"
        strokeWidth="1.4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="10" cy="10" r="2.2" stroke="currentColor" strokeWidth="1.5" />
    </svg>
  )
}

function TrashIcon() {
  return (
    <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
      <path
        d="M6.5 5.5h7m-6 0V4.75A1.75 1.75 0 0 1 9.25 3h1.5A1.75 1.75 0 0 1 12.5 4.75v.75m-8 0h11m-1 0-.6 8.39a1.75 1.75 0 0 1-1.75 1.61H7.85A1.75 1.75 0 0 1 6.1 13.89L5.5 5.5m2.75 2.5v4m4-4v4"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function CardActionButton({ labelKey, className, icon, onClick }: {
  labelKey: string
  className?: string
  icon: ReactNode
  onClick: () => void
}) {
  const { t } = useTranslation()
  return (
    <button
      type="button"
      className={className}
      onClick={onClick}
      title={t(labelKey)}
      aria-label={t(labelKey)}
    >
      {icon}
    </button>
  )
}

interface McpCardProps {
  entry: McpEntry
  onToggle: (name: string, enabled: boolean) => void
  onEdit?: (entry: McpEntry) => void
  onConfigKnowledge?: (entry: McpEntry) => void
  onDelete?: (name: string) => void
  isCustom?: boolean
}

export default function McpCard({ entry, onToggle, onEdit, onConfigKnowledge, onDelete, isCustom }: McpCardProps) {
  const { t } = useTranslation()
  const displayName = getMcpDisplayName(entry)

  const handleToggle = () => {
    onToggle(entry.name, !entry.enabled)
  }

  return (
    <div className={`mcp-card ${entry.enabled ? 'mcp-card-enabled' : ''}`}>
      <div className="mcp-card-header">
        <div className="mcp-card-title">
          <span className="mcp-card-name">{displayName}</span>
          {isCustom && <span className="mcp-card-badge">{t('mcp.customBadge')}</span>}
        </div>
        <label className="mcp-toggle">
          <input
            type="checkbox"
            checked={entry.enabled}
            onChange={handleToggle}
          />
          <span className="mcp-toggle-slider"></span>
        </label>
      </div>

      <p className="mcp-card-description">
        {entry.description || t('mcp.noDescription')}
      </p>

      {(isCustom || onConfigKnowledge) && (onEdit || onConfigKnowledge || onDelete) && (
        <div className="mcp-card-actions">
          {onConfigKnowledge && (
            <CardActionButton
              labelKey="mcp.configKnowledge"
              className="card-icon-action"
              icon={<ConfigureIcon />}
              onClick={() => onConfigKnowledge(entry)}
            />
          )}
          {onEdit && (
            <CardActionButton
              labelKey="common.edit"
              className="card-icon-action"
              icon={<EditIcon />}
              onClick={() => onEdit(entry)}
            />
          )}
          {onDelete && (
            <CardActionButton
              labelKey="common.delete"
              className="card-icon-action card-icon-action-danger"
              icon={<TrashIcon />}
              onClick={() => onDelete(entry.name)}
            />
          )}
        </div>
      )}
    </div>
  )
}
