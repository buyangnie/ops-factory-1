import { describe, it, expect } from 'vitest'

/**
 * Tests for the markInherited algorithm that computes inheritedDisabled
 * on resource tree nodes (HostResourcePage.tsx).
 *
 * The function is re-created here for pure unit testing since it is
 * embedded inside a useMemo and not directly exportable.
 */

type TreeNodeType = 'group' | 'subgroup' | 'business-service' | 'cluster'

type TreeNode = {
    id: string
    type: TreeNodeType
    name: string
    children?: TreeNode[]
    raw?: { enabled?: boolean }
    inheritedDisabled?: boolean
}

function markInherited(nodes: TreeNode[], ancestorOff: boolean) {
    for (const n of nodes) {
        const isGroup = n.type === 'group' || n.type === 'subgroup'
        const groupEnabled = isGroup && n.raw ? n.raw.enabled !== false : true
        const effective = ancestorOff || !groupEnabled
        n.inheritedDisabled = effective
        if (n.children) markInherited(n.children, effective)
    }
}

describe('markInherited', () => {
    it('marks all nodes as enabled when no group is disabled', () => {
        const tree: TreeNode[] = [
            {
                id: 'g1', type: 'group', name: 'G1', raw: { enabled: true },
                children: [
                    { id: 'c1', type: 'cluster', name: 'C1' },
                ],
            },
        ]
        markInherited(tree, false)
        expect(tree[0].inheritedDisabled).toBe(false)
        expect(tree[0].children![0].inheritedDisabled).toBe(false)
    })

    it('marks a disabled group and its children', () => {
        const tree: TreeNode[] = [
            {
                id: 'g1', type: 'group', name: 'G1', raw: { enabled: false },
                children: [
                    { id: 'c1', type: 'cluster', name: 'C1' },
                ],
            },
        ]
        markInherited(tree, false)
        expect(tree[0].inheritedDisabled).toBe(true)
        expect(tree[0].children![0].inheritedDisabled).toBe(true)
    })

    it('inherits disabled from ancestor through multiple levels', () => {
        const tree: TreeNode[] = [
            {
                id: 'g1', type: 'group', name: 'G1', raw: { enabled: false },
                children: [
                    {
                        id: 'sg1', type: 'subgroup', name: 'SG1', raw: { enabled: true },
                        children: [
                            { id: 'c1', type: 'cluster', name: 'C1' },
                        ],
                    },
                ],
            },
        ]
        markInherited(tree, false)
        expect(tree[0].inheritedDisabled).toBe(true)
        expect(tree[0].children![0].inheritedDisabled).toBe(true)
        expect(tree[0].children![0].children![0].inheritedDisabled).toBe(true)
    })

    it('treats missing enabled field as enabled (not disabled)', () => {
        const tree: TreeNode[] = [
            { id: 'g1', type: 'group', name: 'G1', raw: {} },
        ]
        markInherited(tree, false)
        expect(tree[0].inheritedDisabled).toBe(false)
    })

    it('treats missing raw as enabled', () => {
        const tree: TreeNode[] = [
            { id: 'c1', type: 'cluster', name: 'C1' },
        ]
        markInherited(tree, false)
        expect(tree[0].inheritedDisabled).toBe(false)
    })

    it('isolates independent root groups', () => {
        const tree: TreeNode[] = [
            {
                id: 'g1', type: 'group', name: 'G1', raw: { enabled: false },
                children: [{ id: 'c1', type: 'cluster', name: 'C1' }],
            },
            {
                id: 'g2', type: 'group', name: 'G2', raw: { enabled: true },
                children: [{ id: 'c2', type: 'cluster', name: 'C2' }],
            },
        ]
        markInherited(tree, false)
        expect(tree[0].inheritedDisabled).toBe(true)
        expect(tree[0].children![0].inheritedDisabled).toBe(true)
        expect(tree[1].inheritedDisabled).toBe(false)
        expect(tree[1].children![0].inheritedDisabled).toBe(false)
    })

    it('allows re-enabling a child does not override ancestor disabled', () => {
        const tree: TreeNode[] = [
            {
                id: 'root', type: 'group', name: 'Root', raw: { enabled: false },
                children: [
                    {
                        id: 'child', type: 'subgroup', name: 'Child', raw: { enabled: true },
                        children: [
                            { id: 'c1', type: 'cluster', name: 'C1' },
                        ],
                    },
                ],
            },
        ]
        markInherited(tree, false)
        // child has enabled=true but inherits disabled from root
        expect(tree[0].children![0].inheritedDisabled).toBe(true)
        expect(tree[0].children![0].children![0].inheritedDisabled).toBe(true)
    })
})
