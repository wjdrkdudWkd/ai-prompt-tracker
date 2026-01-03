import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils/cn";

interface KPICardProps {
  title: string;
  value: string | number;
  icon: LucideIcon;
  trend?: {
    value: number;
    isPositive: boolean;
  };
  subtitle?: string;
  loading?: boolean;
}

export function KPICard({ title, value, icon: Icon, trend, subtitle, loading }: KPICardProps) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
        <Icon className="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="h-8 w-32 animate-pulse rounded bg-muted" />
        ) : (
          <>
            <div className="text-2xl font-bold">{value}</div>
            {trend && (
              <p className={cn("text-xs", trend.isPositive ? "text-green-500" : "text-red-500")}>
                {trend.isPositive ? "+" : ""}
                {trend.value.toFixed(1)}%
                {subtitle && <span className="text-muted-foreground"> {subtitle}</span>}
              </p>
            )}
            {subtitle && !trend && <p className="text-xs text-muted-foreground">{subtitle}</p>}
          </>
        )}
      </CardContent>
    </Card>
  );
}
