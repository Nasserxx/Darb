import { Link, useLocation, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { BellIcon } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  useMarkNotificationAsRead,
  useMyNotifications,
} from "@/features/notifications/hooks/use-notifications.ts";
import { cn } from "@/lib/utils.ts";

export function NotificationsBell() {
  const { t } = useTranslation("app");
  const { locale } = useParams<{ locale: string }>();
  const location = useLocation();
  const localePrefix = locale ?? "en";
  const notificationsHref = `/${localePrefix}/notifications`;
  const isActive = location.pathname.startsWith(notificationsHref);

  const { data } = useMyNotifications({ page: 0, size: 8 });
  const markRead = useMarkNotificationAsRead();
  const items = data?.content ?? [];

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="outline"
          size="icon-sm"
          data-testid="nav-notifications"
          aria-label={t("notifications.openMenu")}
          className={cn(isActive && "bg-muted")}
        >
          <BellIcon />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-80">
        {items.length === 0 ? (
          <p className="px-2 py-6 text-center text-sm text-muted-foreground">
            {t("notifications.sheetEmpty")}
          </p>
        ) : (
          items.map((item) => (
            <DropdownMenuItem
              key={item.id}
              className="flex flex-col items-start gap-0.5 py-2"
              onSelect={() => {
                // ponytail: silent mark-read; toast lives on full page
                if (item.status !== "READ") {
                  void markRead.mutateAsync(item.id);
                }
              }}
            >
              <span className="font-medium">{item.title}</span>
              <span className="line-clamp-2 text-xs text-muted-foreground">
                {item.body}
              </span>
            </DropdownMenuItem>
          ))
        )}
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link to={notificationsHref}>{t("notifications.viewAll")}</Link>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
