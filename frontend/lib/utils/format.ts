import { format, formatDistanceToNow } from "date-fns";

/**
 * Format currency (USD)
 */
export function formatCurrency(value: number | null | undefined): string {
  if (value === null || value === undefined) return "-";
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  }).format(value);
}

/**
 * Format number with thousand separators
 */
export function formatNumber(value: number | null | undefined): string {
  if (value === null || value === undefined) return "-";
  return new Intl.NumberFormat("en-US").format(value);
}

/**
 * Format percentage
 */
export function formatPercentage(value: number | null | undefined): string {
  if (value === null || value === undefined) return "-";
  return `${(value * 100).toFixed(1)}%`;
}

/**
 * Format latency (milliseconds)
 */
export function formatLatency(ms: number | null | undefined): string {
  if (ms === null || ms === undefined) return "-";
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(2)}s`;
}

/**
 * Format duration
 */
export function formatDuration(ms: number | null | undefined): string {
  if (ms === null || ms === undefined) return "-";
  if (ms < 1000) return `${ms}ms`;
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`;
  const minutes = Math.floor(ms / 60000);
  const seconds = ((ms % 60000) / 1000).toFixed(0);
  return `${minutes}m ${seconds}s`;
}

/**
 * Format ISO timestamp to readable format
 */
export function formatDateTime(isoString: string | null | undefined): string {
  if (!isoString) return "-";
  try {
    return format(new Date(isoString), "MMM d, yyyy HH:mm:ss");
  } catch {
    return isoString;
  }
}

/**
 * Format ISO timestamp to short date
 */
export function formatDate(isoString: string | null | undefined): string {
  if (!isoString) return "-";
  try {
    return format(new Date(isoString), "MMM d, yyyy");
  } catch {
    return isoString;
  }
}

/**
 * Format ISO timestamp to relative time
 */
export function formatRelativeTime(isoString: string | null | undefined): string {
  if (!isoString) return "-";
  try {
    return formatDistanceToNow(new Date(isoString), { addSuffix: true });
  } catch {
    return isoString;
  }
}

/**
 * Format tokens
 */
export function formatTokens(tokens: number | null | undefined): string {
  if (tokens === null || tokens === undefined) return "-";
  if (tokens < 1000) return tokens.toString();
  if (tokens < 1000000) return `${(tokens / 1000).toFixed(1)}K`;
  return `${(tokens / 1000000).toFixed(1)}M`;
}

/**
 * Get status color
 */
export function getStatusColor(status: string): string {
  switch (status?.toLowerCase()) {
    case "success":
      return "text-green-500";
    case "error":
    case "failed":
      return "text-red-500";
    case "pending":
      return "text-yellow-500";
    default:
      return "text-gray-500";
  }
}

/**
 * Get status badge variant
 */
export function getStatusBadgeVariant(status: string): "default" | "success" | "destructive" | "secondary" {
  switch (status?.toLowerCase()) {
    case "success":
      return "success";
    case "error":
    case "failed":
      return "destructive";
    case "pending":
      return "secondary";
    default:
      return "default";
  }
}
