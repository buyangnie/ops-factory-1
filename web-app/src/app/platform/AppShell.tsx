import type { ReactNode } from 'react'

export function AppShell({
    isEmbed,
    isCollapsed,
    isRightPanelOpen,
    rightPanelMode,
    sidebar,
    children,
    rightPanel,
}: {
    isEmbed: boolean
    isCollapsed: boolean
    isRightPanelOpen: boolean
    rightPanelMode: string
    sidebar: ReactNode
    children: ReactNode
    rightPanel: ReactNode
}) {
    const mainWrapperClass = [
        'main-wrapper',
        isEmbed ? 'embed-mode' : (isCollapsed ? 'sidebar-collapsed' : ''),
        isRightPanelOpen ? 'with-right-panel' : '',
        rightPanelMode,
    ].filter(Boolean).join(' ')

    return (
        <div className="app-container">
            {!isEmbed && sidebar}
            <div className={mainWrapperClass}>
                <main className="main-content">{children}</main>
                {rightPanel}
            </div>
        </div>
    )
}

