/**
 * Base API client with fetch wrapper
 *
 * API_BASE_URL Configuration:
 * - Embedded mode (default): "" (same-origin, calls /aiprompt-tracker/api/*)
 * - Standalone dev: "http://localhost:8080"
 * - Standalone prod: Set NEXT_PUBLIC_API_BASE_URL env var
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "";

export class ApiError extends Error {
  constructor(
    public status: number,
    public statusText: string,
    message: string
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/**
 * Generic fetch wrapper with error handling
 */
async function apiFetch<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const url = `${API_BASE_URL}${endpoint}`;

  try {
    const response = await fetch(url, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...options?.headers,
      },
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new ApiError(
        response.status,
        response.statusText,
        errorText || `HTTP ${response.status}: ${response.statusText}`
      );
    }

    // Handle empty responses (204 No Content)
    if (response.status === 204) {
      return null as T;
    }

    const data = await response.json();
    return data as T;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new Error(`Network error: ${error instanceof Error ? error.message : "Unknown error"}`);
  }
}

/**
 * GET request
 */
export async function get<T>(endpoint: string): Promise<T> {
  return apiFetch<T>(endpoint, { method: "GET" });
}

/**
 * POST request
 */
export async function post<T>(endpoint: string, data?: any): Promise<T> {
  return apiFetch<T>(endpoint, {
    method: "POST",
    body: data ? JSON.stringify(data) : undefined,
  });
}

/**
 * PUT request
 */
export async function put<T>(endpoint: string, data?: any): Promise<T> {
  return apiFetch<T>(endpoint, {
    method: "PUT",
    body: data ? JSON.stringify(data) : undefined,
  });
}

/**
 * DELETE request
 */
export async function del<T>(endpoint: string): Promise<T> {
  return apiFetch<T>(endpoint, { method: "DELETE" });
}
