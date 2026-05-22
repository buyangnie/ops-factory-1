/**
 * Input validation utilities to prevent XSS injection.
 *
 * WARNING: These utilities provide basic XSS protection for specific contexts.
 * For production use, consider using a comprehensive security library like DOMPurify.
 */

/**
 * Characters that need escaping in HTML content context.
 */
const HTML_CONTENT_ESCAPE_CHARS: Readonly<Record<string, string>> = {
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#x27;',
    '&': '&amp;',
    '`': '&#x60;', // Modern XSS vectors use backticks
    '/': '&#x2F;', // Can be used in closing tags
} as const

/**
 * Regex for detecting potentially dangerous characters in different contexts.
 */
const XSS_PATTERNS = {
    htmlContent: /[<>"'&`/]/,
    javascript: /["'\\`]|<script|javascript:|on\w+\s*=/i,
    url: /[<>"'`]|javascript:|data:|vbscript:/i,
} as const

/**
 * Check if input contains XSS characters.
 */
export function hasXssChars(input: string): boolean {
    return XSS_PATTERNS.htmlContent.test(input)
}

/**
 * Sanitize input for HTML content context.
 *
 * Use this when inserting user input as text content inside HTML elements:
 * <div>{sanitizeHtmlContent(userInput)}</div>
 *
 * @param input - The input string to sanitize
 * @returns Sanitized string safe for HTML content context
 */
export function sanitizeHtmlContent(input: string): string {
    return input.replace(XSS_PATTERNS.htmlContent, (char) => {
        return HTML_CONTENT_ESCAPE_CHARS[char] || char
    })
}

/**
 * Validate input for JavaScript context.
 *
 * This checks if the input contains potentially dangerous patterns.
 * Returns false if the input is unsafe for JavaScript context.
 *
 * @param input - The input string to validate
 * @returns true if safe, false if potentially dangerous
 */
export function isSafeForJavaScript(input: string): boolean {
    // Check for dangerous patterns
    if (XSS_PATTERNS.javascript.test(input)) {
        return false
    }
    // Check for unicode escape sequences that could be decoded
    if (/\\u[0-9a-fA-F]{4}/.test(input)) {
        return false
    }
    return true
}

/**
 * Validate input for URL context.
 *
 * This checks if the input contains potentially dangerous URL patterns.
 * Returns false if the input is unsafe for URL context.
 *
 * @param input - The input string to validate
 * @returns true if safe, false if potentially dangerous
 */
export function isSafeForUrl(input: string): boolean {
    return !XSS_PATTERNS.url.test(input)
}

/**
 * Validate and sanitize input with field name.
 *
 * @param input - The input string to validate
 * @param fieldName - The field name for error messages
 * @returns Validation result with sanitized value and safety status
 */
export function validateAndSanitize(input: string, fieldName: string): { valid: boolean; sanitized: string; error?: string } {
    const trimmed = input.trim()
    if (hasXssChars(trimmed)) {
        return {
            valid: false,
            sanitized: sanitizeHtmlContent(trimmed),
            error: fieldName + ' contains invalid characters (< > " \' & ` /). These characters are not allowed for security reasons.'
        }
    }
    return {
        valid: true,
        sanitized: trimmed
    }
}

/**
 * Sanitize input (legacy function, kept for backward compatibility).
 *
 * @deprecated Use sanitizeHtmlContent instead.
 * @param input - The input string to sanitize
 * @returns Sanitized string
 */
export function sanitizeInput(input: string): string {
    return sanitizeHtmlContent(input)
}