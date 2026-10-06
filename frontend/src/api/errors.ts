import type { TFunction } from 'i18next'

export interface FieldError {
  field: string
  code?: string
  message?: string
}

/** Błąd API w formacie RFC 7807 (ProblemDetail) z dodatkowymi polami `code` i `errors`. */
export interface Problem {
  status: number
  title?: string
  detail?: string
  code?: string
  errors?: FieldError[]
  [key: string]: unknown
}

export class ApiError extends Error {
  readonly problem: Problem

  constructor(problem: Problem) {
    super(problem.detail ?? problem.title ?? `HTTP ${problem.status}`)
    this.name = 'ApiError'
    this.problem = problem
  }

  get status() {
    return this.problem.status
  }

  get code() {
    return this.problem.code
  }
}

export function toProblem(error: unknown, response: Response): Problem {
  if (error && typeof error === 'object') {
    return { status: response.status, ...(error as object) } as Problem
  }
  return { status: response.status, code: 'http_error', detail: String(error ?? response.statusText) }
}

/** Rozpakowuje wynik openapi-fetch: zwraca dane albo rzuca ApiError. */
export async function unwrap<T>(
  promise: Promise<{ data?: T; error?: unknown; response: Response }>,
): Promise<T> {
  const { data, error, response } = await promise
  if (!response.ok) {
    throw new ApiError(toProblem(error, response))
  }
  return data as T
}

/** Komunikat błędu dla użytkownika – tłumaczony po `code`, z ogólnym fallbackiem. */
export function errorMessage(t: TFunction, error: unknown): string {
  if (error instanceof ApiError) {
    const key = `errors.${error.code}`
    return t(key, { defaultValue: t('errors.generic') })
  }
  return t('errors.network')
}

const VALIDATION_CODES: Record<string, string> = {
  NotBlank: 'validation.required',
  NotNull: 'validation.required',
  Email: 'validation.email',
  Size: 'validation.length',
  Pattern: 'validation.invalid',
  Min: 'validation.invalid',
  Max: 'validation.invalid',
}

/** Przenosi błędy walidacji z backendu na pola formularza react-hook-form. */
export function applyFieldErrors(
  error: unknown,
  setError: (field: never, error: { message: string }) => void,
): boolean {
  if (!(error instanceof ApiError) || !error.problem.errors?.length) {
    return false
  }
  for (const fe of error.problem.errors) {
    setError(fe.field as never, { message: VALIDATION_CODES[fe.code ?? ''] ?? 'validation.invalid' })
  }
  return true
}
