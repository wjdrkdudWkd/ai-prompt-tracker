/**
 * Build URL query string from filters object
 */
export function buildQueryString(filters: Record<string, any>): string {
  const params = new URLSearchParams();

  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.set(key, value.toString());
    }
  });

  const queryString = params.toString();
  return queryString ? `?${queryString}` : "";
}

/**
 * Parse URL query params to filters object
 */
export function parseQueryParams<T>(searchParams: URLSearchParams): Partial<T> {
  const filters: Record<string, any> = {};

  searchParams.forEach((value, key) => {
    // Convert page/size to numbers
    if (key === "page" || key === "size") {
      filters[key] = parseInt(value, 10);
    } else {
      filters[key] = value;
    }
  });

  return filters as Partial<T>;
}

/**
 * Get default date range (last 7 days)
 */
export function getDefaultDateRange(): { from: string; to: string } {
  const to = new Date();
  const from = new Date();
  from.setDate(from.getDate() - 7);

  return {
    from: from.toISOString().split("T")[0],
    to: to.toISOString().split("T")[0],
  };
}

/**
 * Get date range for preset
 */
export function getDateRangeForPreset(preset: "7d" | "30d" | "90d"): { from: string; to: string } {
  const to = new Date();
  const from = new Date();

  switch (preset) {
    case "7d":
      from.setDate(from.getDate() - 7);
      break;
    case "30d":
      from.setDate(from.getDate() - 30);
      break;
    case "90d":
      from.setDate(from.getDate() - 90);
      break;
  }

  return {
    from: from.toISOString().split("T")[0],
    to: to.toISOString().split("T")[0],
  };
}
