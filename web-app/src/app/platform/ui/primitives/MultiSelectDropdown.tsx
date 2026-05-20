import { useState, useEffect, useRef } from 'react'
import './MultiSelectDropdown.css'

interface MultiSelectOption {
    value: string
    label: string
}

export function MultiSelectDropdown({ options, selectedIds, onChange, placeholder }: {
    options: MultiSelectOption[]
    selectedIds: string[]
    onChange: (ids: string[]) => void
    placeholder: string
}) {
    const [open, setOpen] = useState(false)
    const ref = useRef<HTMLDivElement>(null)

    useEffect(() => {
        if (!open) return
        const handler = (e: MouseEvent) => {
            if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
        }
        document.addEventListener('mousedown', handler)
        return () => document.removeEventListener('mousedown', handler)
    }, [open])

    const selectedLabels = options.filter(o => selectedIds.includes(o.value))

    return (
        <div className="multiselect" ref={ref}>
            <div className="multiselect-trigger" onClick={() => setOpen(v => !v)}>
                {selectedLabels.length === 0
                    ? <span className="multiselect-placeholder">{placeholder}</span>
                    : <div className="multiselect-tags">
                        {selectedLabels.map(o => (
                            <span key={o.value} className="multiselect-tag">
                                {o.label}
                                <span className="multiselect-tag-remove" onClick={e => { e.stopPropagation(); onChange(selectedIds.filter(id => id !== o.value)) }}>×</span>
                            </span>
                        ))}
                    </div>
                }
                <span className={`multiselect-arrow ${open ? 'is-open' : ''}`}>▾</span>
            </div>
            {open && (
                <div className="multiselect-dropdown">
                    {options.map(o => {
                        const checked = selectedIds.includes(o.value)
                        return (
                            <div key={o.value} className={`multiselect-option ${checked ? 'is-selected' : ''}`} onClick={() => {
                                onChange(checked ? selectedIds.filter(id => id !== o.value) : [...selectedIds, o.value])
                            }}>
                                <span className={`multiselect-check ${checked ? 'is-checked' : ''}`} />
                                {o.label}
                            </div>
                        )
                    })}
                    {options.length === 0 && <div className="multiselect-empty">{placeholder}</div>}
                </div>
            )}
        </div>
    )
}
