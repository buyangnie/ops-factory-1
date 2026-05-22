import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { HostGroup, Cluster, BusinessService } from '../../../../types/host'
import TopologyNodeIcon from './TopologyNodeIcon'

export type TreeNodeType = 'group' | 'subgroup' | 'business-service' | 'cluster'

export type TreeNode = {
    id: string
    type: TreeNodeType
    name: string
    subtitle?: string
    children?: TreeNode[]
    raw?: HostGroup | Cluster | BusinessService
    inheritedDisabled?: boolean
}

type Props = {
    tree: TreeNode[]
    selectedId: string | null
    selectedType: TreeNodeType | null
    selectedIds?: Set<string>
    onSelect: (id: string, type: TreeNodeType) => void
    onToggleSelect?: (id: string, type: TreeNodeType) => void
    onEdit?: (id: string, type: TreeNodeType) => void
    onDelete?: (id: string, type: TreeNodeType) => void
}

export default function ResourceTree({ tree, selectedId, selectedType, selectedIds, onSelect, onToggleSelect, onEdit, onDelete }: Props) {
    const { t } = useTranslation()

    if (tree.length === 0) {
        return (
            <div className="hr-tree-empty">
                {t('hostResource.noGroups')}
            </div>
        )
    }

    return (
        <div className="hr-tree">
            {tree.map(node => (
                <TreeNodeItem
                    key={node.id}
                    node={node}
                    depth={0}
                    selectedId={selectedId}
                    selectedType={selectedType}
                    selectedIds={selectedIds}
                    onSelect={onSelect}
                    onToggleSelect={onToggleSelect}
                    onEdit={onEdit}
                    onDelete={onDelete}
                    inheritedDisabled={false}
                />
            ))}
        </div>
    )
}

function TreeNodeItem({ node, depth, selectedId, selectedType, selectedIds, onSelect, onToggleSelect, onEdit, onDelete, inheritedDisabled }: {
    node: TreeNode
    depth: number
    selectedId: string | null
    selectedType: TreeNodeType | null
    selectedIds?: Set<string>
    onSelect: (id: string, type: TreeNodeType) => void
    onToggleSelect?: (id: string, type: TreeNodeType) => void
    onEdit?: (id: string, type: TreeNodeType) => void
    onDelete?: (id: string, type: TreeNodeType) => void
    inheritedDisabled: boolean
}) {
    const isSelected = selectedId === node.id && selectedType === node.type
    const isChecked = selectedIds ? selectedIds.has(node.id) : false
    const hasChildren = node.children && node.children.length > 0
    const [expanded, setExpanded] = useState(false)

    const isDisabled = inheritedDisabled === true
    const isCluster = node.type === 'cluster' || node.type === 'business-service'

    const handleClick = () => {
        onSelect(node.id, node.type)
    }

    const handleToggle = (e: React.MouseEvent) => {
        e.stopPropagation()
        setExpanded(prev => !prev)
    }

    const handleCheckboxClick = (e: React.ChangeEvent<HTMLInputElement>) => {
        e.stopPropagation()
        if (onToggleSelect && isCluster) {
            onToggleSelect(node.id, node.type)
        }
    }

    const isFolder = node.type === 'group' || node.type === 'subgroup'

    return (
        <div className="hr-tree-node-wrapper">
            <div
                className={`hr-tree-node ${isSelected ? 'hr-tree-node-selected' : ''} ${isChecked ? 'hr-tree-node-checked' : ''} ${isDisabled ? 'hr-tree-node-disabled' : ''}`}
                style={{ paddingLeft: depth * 16 + 8 }}
                onClick={handleClick}
            >
                {isFolder && (
                    <span
                        className={`hr-tree-chevron ${expanded ? 'hr-tree-chevron-open' : ''}`}
                        onClick={handleToggle}
                        style={{ opacity: hasChildren ? 1 : 0.3 }}
                    >
                        &#9654;
                    </span>
                )}
                {isCluster && selectedIds && (
                    <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={handleCheckboxClick}
                        style={{ marginRight: 6, cursor: 'pointer' }}
                        onClick={e => e.stopPropagation()}
                    />
                )}
                {isFolder ? (
                    <span className="hr-tree-icon hr-tree-icon-folder" />
                ) : (
                    <TopologyNodeIcon
                        kind={node.type === 'business-service' ? 'business' : 'cluster'}
                        size={16}
                        className={`hr-tree-icon hr-tree-icon-${node.type}`}
                    />
                )}
                <span className="hr-tree-label">{node.name}</span>
                {node.subtitle && <span className="hr-tree-subtitle">{node.subtitle}</span>}
                <span className="hr-tree-node-actions" onClick={e => e.stopPropagation()}>
                    {onEdit && (
                        <button
                            className="hr-tree-node-action"
                            title="Edit"
                            onClick={() => onEdit(node.id, node.type)}
                        >
                            &#9998;
                        </button>
                    )}
                    {onDelete && (
                        <button
                            className="hr-tree-node-action hr-tree-node-action-danger"
                            title="Delete"
                            onClick={() => onDelete(node.id, node.type)}
                        >
                            &#128465;
                        </button>
                    )}
                </span>
            </div>
            {hasChildren && expanded && (
                <div className="hr-tree-children">
                    {node.children!.map(child => (
                        <TreeNodeItem
                            key={child.id}
                            node={child}
                            depth={depth + 1}
                            selectedId={selectedId}
                            selectedType={selectedType}
                            onSelect={onSelect}
                            onEdit={onEdit}
                            onDelete={onDelete}
                            inheritedDisabled={isDisabled}
                        />
                    ))}
                </div>
            )}
        </div>
    )
}
