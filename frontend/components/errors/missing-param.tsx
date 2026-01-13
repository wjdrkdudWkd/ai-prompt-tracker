"use client";

import { AlertCircle, ArrowLeft } from "lucide-react";
import Link from "next/link";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

interface MissingParamErrorProps {
  /**
   * The name of the missing parameter
   */
  paramName: string;

  /**
   * URL to navigate back to
   * @default "/aiprompt-tracker/functions"
   */
  backUrl?: string;

  /**
   * Custom back link text
   * @default "Back to Functions"
   */
  backText?: string;

  /**
   * Custom error message
   */
  message?: string;
}

/**
 * Error component displayed when a required query parameter is missing
 *
 * @example
 * ```tsx
 * const functionName = useRequiredQueryParam("name", { autoRedirect: false });
 * if (!functionName) {
 *   return <MissingParamError paramName="name" />;
 * }
 * ```
 */
export function MissingParamError({
  paramName,
  backUrl = "/aiprompt-tracker/functions",
  backText = "Back to Functions",
  message,
}: MissingParamErrorProps) {
  return (
    <div className="flex min-h-[60vh] items-center justify-center p-8">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-destructive/10 p-2">
              <AlertCircle className="h-6 w-6 text-destructive" />
            </div>
            <div>
              <CardTitle>Missing Parameter</CardTitle>
              <CardDescription>Required query parameter not found</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="rounded-lg bg-muted p-4">
            <p className="text-sm text-muted-foreground">
              {message || (
                <>
                  The required query parameter <code className="rounded bg-background px-1.5 py-0.5 font-mono text-sm">?{paramName}=...</code> is missing from the URL.
                </>
              )}
            </p>
          </div>

          <div className="text-sm text-muted-foreground">
            <p className="mb-2 font-medium">Expected URL format:</p>
            <code className="block rounded bg-muted p-2 font-mono text-xs">
              /aiprompt-tracker/functions/detail?{paramName}=value
            </code>
          </div>

          <Link
            href={backUrl}
            className="inline-flex items-center gap-2 text-sm font-medium text-primary transition-colors hover:text-primary/80"
          >
            <ArrowLeft className="h-4 w-4" />
            {backText}
          </Link>
        </CardContent>
      </Card>
    </div>
  );
}

/**
 * Generic error component for invalid query parameters
 */
interface InvalidParamErrorProps {
  paramName: string;
  paramValue: string;
  expectedFormat?: string;
  backUrl?: string;
  backText?: string;
}

export function InvalidParamError({
  paramName,
  paramValue,
  expectedFormat,
  backUrl = "/aiprompt-tracker/functions",
  backText = "Back to Functions",
}: InvalidParamErrorProps) {
  return (
    <div className="flex min-h-[60vh] items-center justify-center p-8">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="rounded-full bg-destructive/10 p-2">
              <AlertCircle className="h-6 w-6 text-destructive" />
            </div>
            <div>
              <CardTitle>Invalid Parameter</CardTitle>
              <CardDescription>The provided parameter value is invalid</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="rounded-lg bg-muted p-4 space-y-2">
            <div className="text-sm">
              <span className="text-muted-foreground">Parameter:</span>{" "}
              <code className="rounded bg-background px-1.5 py-0.5 font-mono text-sm">{paramName}</code>
            </div>
            <div className="text-sm">
              <span className="text-muted-foreground">Value:</span>{" "}
              <code className="rounded bg-background px-1.5 py-0.5 font-mono text-sm">{paramValue}</code>
            </div>
            {expectedFormat && (
              <div className="text-sm">
                <span className="text-muted-foreground">Expected:</span>{" "}
                <span className="text-xs">{expectedFormat}</span>
              </div>
            )}
          </div>

          <Link
            href={backUrl}
            className="inline-flex items-center gap-2 text-sm font-medium text-primary transition-colors hover:text-primary/80"
          >
            <ArrowLeft className="h-4 w-4" />
            {backText}
          </Link>
        </CardContent>
      </Card>
    </div>
  );
}
