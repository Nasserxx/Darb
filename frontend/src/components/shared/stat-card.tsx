import type { LucideIcon } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";

type StatCardProps = {
  title: string;
  value: string | number;
  description?: string;
  icon?: LucideIcon;
  accent?: "default" | "gold";
  className?: string;
};

export function StatCard({
  title,
  value,
  description,
  icon: Icon,
  accent = "default",
  className,
}: StatCardProps) {
  return (
    <Card className={cn("overflow-hidden", className)}>
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          {title}
        </CardTitle>
        {Icon ? (
          <Icon
            className={cn(
              accent === "gold" ? "text-accent" : "text-primary",
            )}
            data-icon="inline-end"
          />
        ) : null}
      </CardHeader>
      <CardContent className="flex flex-col gap-1">
        <p className="font-serif text-3xl font-semibold tabular-nums">{value}</p>
        {description ? (
          <p className="text-xs text-muted-foreground">{description}</p>
        ) : null}
      </CardContent>
    </Card>
  );
}
