"use client";

import { useSearchParams, useRouter } from "next/navigation";
import { useEffect } from "react";

/**
 * Query Parameter Hook Options
 */
interface QueryParamOptions {
  /**
   * Fallback URL to redirect to if required param is missing
   * @default "/aiprompt-tracker/functions"
   */
  fallbackUrl?: string;

  /**
   * Whether to automatically redirect on missing param
   * If false, returns null and lets component handle UI
   * @default true
   */
  autoRedirect?: boolean;

  /**
   * Default value to use if param is missing (overrides redirect)
   */
  defaultValue?: string;
}

/**
 * Hook to safely read an optional query parameter
 *
 * @param key - Query parameter key
 * @returns The parameter value or null if not present
 *
 * @example
 * ```tsx
 * const filter = useOptionalQueryParam("filter");
 * // filter is string | null
 * ```
 */
export function useOptionalQueryParam(key: string): string | null {
  const searchParams = useSearchParams();

  // Null-safe: useSearchParams() can return null during SSR/initial render
  if (!searchParams) {
    return null;
  }

  return searchParams.get(key);
}

/**
 * Hook to safely read a required query parameter with automatic error handling
 *
 * @param key - Query parameter key (required)
 * @param options - Configuration options
 * @returns The parameter value (guaranteed non-null) or null during redirect
 *
 * @example
 * ```tsx
 * // Automatic redirect on missing param
 * const functionName = useRequiredQueryParam("name");
 * if (!functionName) return null; // Will redirect automatically
 *
 * // Custom fallback
 * const id = useRequiredQueryParam("id", {
 *   fallbackUrl: "/aiprompt-tracker/dashboard"
 * });
 *
 * // With default value (no redirect)
 * const page = useRequiredQueryParam("page", {
 *   defaultValue: "1"
 * });
 * ```
 */
export function useRequiredQueryParam(
  key: string,
  options: QueryParamOptions = {}
): string | null {
  const {
    fallbackUrl = "/aiprompt-tracker/functions",
    autoRedirect = true,
    defaultValue,
  } = options;

  const searchParams = useSearchParams();
  const router = useRouter();

  // Null-safe: handle SSR/initial render
  if (!searchParams) {
    return null;
  }

  const value = searchParams.get(key);

  // If value exists, return it
  if (value) {
    return value;
  }

  // If default value provided, use it (no redirect)
  if (defaultValue !== undefined) {
    return defaultValue;
  }

  // If auto-redirect enabled, redirect to fallback
  useEffect(() => {
    if (autoRedirect && !value) {
      console.warn(
        `[useRequiredQueryParam] Missing required param: ${key}. Redirecting to ${fallbackUrl}`
      );
      router.push(fallbackUrl);
    }
  }, [value, autoRedirect, key, fallbackUrl, router]);

  // Return null during redirect phase
  return null;
}

/**
 * Hook to read multiple query parameters at once
 *
 * @param keys - Array of parameter keys
 * @returns Record of key-value pairs (values are string | null)
 *
 * @example
 * ```tsx
 * const { provider, model, status } = useQueryParams(["provider", "model", "status"]);
 * ```
 */
export function useQueryParams(
  keys: string[]
): Record<string, string | null> {
  const searchParams = useSearchParams();

  if (!searchParams) {
    return keys.reduce((acc, key) => {
      acc[key] = null;
      return acc;
    }, {} as Record<string, string | null>);
  }

  return keys.reduce((acc, key) => {
    acc[key] = searchParams.get(key);
    return acc;
  }, {} as Record<string, string | null>);
}

/**
 * Type-safe query parameter reader with validation
 *
 * @param key - Query parameter key
 * @param validator - Function to validate/transform the value
 * @param defaultValue - Default value if param is missing or invalid
 * @returns Validated parameter value
 *
 * @example
 * ```tsx
 * const page = useValidatedQueryParam(
 *   "page",
 *   (val) => {
 *     const num = parseInt(val, 10);
 *     return !isNaN(num) && num > 0 ? num : null;
 *   },
 *   1
 * );
 * // page is number (guaranteed)
 * ```
 */
export function useValidatedQueryParam<T>(
  key: string,
  validator: (value: string) => T | null,
  defaultValue: T
): T {
  const searchParams = useSearchParams();

  if (!searchParams) {
    return defaultValue;
  }

  const rawValue = searchParams.get(key);

  if (!rawValue) {
    return defaultValue;
  }

  const validated = validator(rawValue);
  return validated !== null ? validated : defaultValue;
}

/**
 * Hook for pagination query params (common pattern)
 *
 * @param defaults - Default page and size
 * @returns Pagination state with validated numbers
 *
 * @example
 * ```tsx
 * const { page, size } = usePaginationParams({ page: 1, size: 20 });
 * ```
 */
export function usePaginationParams(defaults: { page: number; size: number }) {
  const page = useValidatedQueryParam(
    "page",
    (val) => {
      const num = parseInt(val, 10);
      return !isNaN(num) && num > 0 ? num : null;
    },
    defaults.page
  );

  const size = useValidatedQueryParam(
    "size",
    (val) => {
      const num = parseInt(val, 10);
      return !isNaN(num) && num > 0 && num <= 100 ? num : null;
    },
    defaults.size
  );

  return { page, size };
}
