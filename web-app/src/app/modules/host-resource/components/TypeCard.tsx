import { useTranslation } from 'react-i18next'
import {
    ResourceCardActionGroup,
    ResourceCardDeleteAction,
    ResourceCardEditAction,
} from '../../../platform/ui/primitives/ResourceCard'

type TypeDef = {
    id: string
    name: string
    code: string
    description: string
    color: string
    knowledge: string
    mode?: 'peer' | 'primary-backup'
    createdAt: string
    updatedAt: string
}

type Props = {
    item: TypeDef
    selected?: boolean
    onSelect?: (item: TypeDef) => void
    onEdit: (item: TypeDef) => void
    onDelete: (item: TypeDef) => void
}

export default function TypeCard({ item, selected, onSelect, onEdit, onDelete }: Props) {
    const { t } = useTranslation()
    return (
        <div className="hr-type-def-card" style={selected ? { border: '2px solid var(--color-primary, #3b82f6)' } : undefined}>
            <div className="hr-type-def-card-header">
                {onSelect && (
                    <input
                        type="checkbox"
                        checked={selected}
                        onChange={() => onSelect(item)}
                        style={{ marginRight: 8, cursor: 'pointer' }}
                    />
                )}
                <span className="hr-type-def-card-color" style={{ background: item.color }} />
                <span className="hr-type-def-card-name">{item.name}</span>
                {item.mode === 'primary-backup' && (
                    <span className="hr-mode-badge">
                        {t('hostResource.clusterModePrimaryBackup')}
                    </span>
                )}
            </div>
            {item.description && (
                <div className="hr-type-def-card-desc hr-type-def-card-desc-truncate" title={item.description}>{item.description}</div>
            )}
            {item.knowledge && (
                <>
                    <div className="hr-type-def-card-knowledge-label">{t('hostResource.knowledge')}</div>
                    <div className="hr-type-def-card-knowledge">{item.knowledge}</div>
                </>
            )}
            <ResourceCardActionGroup className="hr-type-def-card-footer">
                <ResourceCardEditAction
                    onClick={() => onEdit(item)}
                    label={t('common.edit')}
                />
                <ResourceCardDeleteAction
                    onClick={() => onDelete(item)}
                    label={t('common.delete')}
                />
            </ResourceCardActionGroup>
        </div>
    )
}
