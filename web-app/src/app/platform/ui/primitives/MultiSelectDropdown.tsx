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
    const [focusIndex, setFocusIndex] = useState(-1)
    const ref = useRef<HTMLDivElement>(null)
    const listRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        if (!open) {
            setFocusIndex(-1)
            return
        }
        const handler = (e: MouseEvent) => {
            if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
        }
        document.addEventListener('mousedown', handler)
        return () => document.removeEventListener('mousedown', handler)
    }, [open])

    useEffect(() => {
        if (focusIndex >= 0 && listRef.current) {
            const items = listRef.current.querySelectorAll<HTMLElement>('[role="option"]')
            items[focusIndex]?.focus()
        }
    }, [focusIndex])

    const handleTriggerKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown') {
            e.preventDefault()
            setOpen(true)
            setFocusIndex(0)
        }
    }

    const handleOptionKeyDown = (e: React.KeyboardEvent, index: number) => {
        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault()
                setFocusIndex(Math.min(index + 1, options.length - 1))
                break
            case 'ArrowUp':
                e.preventDefault()
                if (index === 0) {
                    setOpen(false)
                    ref.current?.querySelector<HTMLElement>('[role="combobox"]')?.focus()
                } else {
                    setFocusIndex(index - 1)
                }
                break
            case 'Enter':
            case ' ': {
                e.preventDefault()
                const o = options[index]
                const checked = selectedIds.includes(o.value)
                onChange(checked ? selectedIds.filter(id => id !== o.value) : [...selectedIds, o.value])
                break
            }
            case 'Escape':
                e.preventDefault()
                setOpen(false)
                ref.current?.querySelector<HTMLElement>('[role="combobox"]')?.focus()
                break
        }
    }

    const selectedLabels = options.filter(o => selectedIds.includes(o.value))

    return (
        <div className="multiselect" ref={ref}>
            <div
                className="multiselect-trigger"
                role="combobox"
                tabIndex={0}
                aria-expanded={open}
                aria-haspopup="listbox"
                onClick={() => setOpen(v => !v)}
                onKeyDown={handleTriggerKeyDown}
            >
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
                <div className="multiselect-dropdown" role="listbox" ref={listRef}>
                    {options.map((o, i) => {
                        const checked = selectedIds.includes(o.value)
                        return (
                            <div
                                key={o.value}
                                role="option"
                                tabIndex={-1}
                                aria-selected={checked}
                                className={`multiselect-option ${checked ? 'is-selected' : ''}`}
                                onClick={() => {
                                    onChange(checked ? selectedIds.filter(id => id !== o.value) : [...selectedIds, o.value])
                                }}
                                onKeyDown={e => handleOptionKeyDown(e, i)}
                            >
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
