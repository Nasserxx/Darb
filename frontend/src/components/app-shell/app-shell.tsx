import type { ReactNode } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";

import { AppLogoLink } from "@/components/app-logo-link.tsx";
import { CapabilityChip } from "@/components/shared/capability-chip.tsx";
import { LocaleSwitcher } from "@/components/locale-switcher.tsx";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarRail,
  SidebarTrigger,
} from "@/components/ui/sidebar";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { translateRole } from "@/i18n/index.ts";
import { getNavItemsForRole } from "@/lib/navigation/app-nav.ts";
import { cn } from "@/lib/utils.ts";
import { LogOutIcon, UserIcon } from "lucide-react";

type AppShellProps = {
  children: ReactNode;
};

export function AppShell({ children }: AppShellProps) {
  const { t } = useTranslation(["app", "auth", "common"]);
  const { locale } = useParams<{ locale: string }>();
  const location = useLocation();
  const { user, logout } = useAuth();
  const localePrefix = locale ?? "en";
  const navItems = user ? getNavItemsForRole(user.role) : [];

  return (
    <SidebarProvider>
      <Sidebar collapsible="icon" className="border-r border-border">
        <SidebarHeader className="brand-panel-pattern border-b border-border/60 px-4 py-4">
          <AppLogoLink size="sm" />
        </SidebarHeader>
        <SidebarContent>
          <SidebarGroup>
            <SidebarGroupLabel>{t("nav.section")}</SidebarGroupLabel>
            <SidebarGroupContent>
              <SidebarMenu>
                {navItems.map((item) => {
                  const href = `/${localePrefix}${item.href}`;
                  const isActive = location.pathname.startsWith(href);
                  return (
                    <SidebarMenuItem key={item.id}>
                      <SidebarMenuButton
                        isActive={isActive}
                        render={<Link to={href} />}
                        tooltip={t(item.labelKey)}
                      >
                        <item.icon />
                        <span>{t(item.labelKey)}</span>
                      </SidebarMenuButton>
                    </SidebarMenuItem>
                  );
                })}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        </SidebarContent>
        <SidebarFooter className="border-t border-border p-2">
          {user ? (
            <p className="truncate px-2 text-xs text-muted-foreground">
              {user.fullName} · {translateRole(user.role)}
            </p>
          ) : null}
        </SidebarFooter>
        <SidebarRail />
      </Sidebar>
      <SidebarInset>
        <header className="flex h-14 shrink-0 items-center justify-between gap-4 border-b border-border bg-card/80 px-4 backdrop-blur-sm">
          <div className="flex min-w-0 items-center gap-2">
            <SidebarTrigger />
            <CapabilityChip className="hidden sm:inline-flex" />
          </div>
          <div className="flex items-center gap-2">
            <LocaleSwitcher />
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" size="sm">
                  <UserIcon data-icon="inline-start" />
                  {user?.fullName ?? t("nav.account")}
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem asChild>
                  <Link to={`/${localePrefix}/settings/profile`}>
                    {t("nav.profile")}
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <Link to={`/${localePrefix}/settings/change-password`}>
                    {t("auth:changePassword.title")}
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={logout}>
                  <LogOutIcon data-icon="inline-start" />
                  {t("auth:dashboard.signOut")}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </header>
        <main className={cn("flex flex-1 flex-col gap-6 p-6")}>{children}</main>
      </SidebarInset>
    </SidebarProvider>
  );
}
