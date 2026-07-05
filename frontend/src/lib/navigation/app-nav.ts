import type { LucideIcon } from "lucide-react";
import {
  Award,
  Bell,
  BookOpen,
  CircleDot,
  ClipboardList,
  CreditCard,
  FileText,
  GraduationCap,
  LayoutDashboard,
  MessageSquare,
  Landmark,
  Settings,
  UserCheck,
  Users,
} from "lucide-react";

import type { UserRole } from "@/lib/types/api.ts";

export type NavItem = {
  id: string;
  labelKey: string;
  href: string;
  icon: LucideIcon;
  roles: UserRole[];
};

export const APP_NAV_ITEMS: NavItem[] = [
  {
    id: "admin",
    labelKey: "nav.admin",
    href: "/admin",
    icon: LayoutDashboard,
    roles: ["SUPER_ADMIN", "MOSQUE_ADMIN"],
  },
  {
    id: "mosques",
    labelKey: "nav.mosques",
    href: "/mosques",
    icon: Landmark,
    roles: ["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER", "STUDENT", "PARENT"],
  },
  {
    id: "mosque-admins",
    labelKey: "nav.mosqueAdmins",
    href: "/mosque-admins",
    icon: UserCheck,
    roles: ["SUPER_ADMIN"],
  },
  {
    id: "teachers",
    labelKey: "nav.teachers",
    href: "/teachers",
    icon: GraduationCap,
    roles: ["SUPER_ADMIN", "MOSQUE_ADMIN"],
  },
  {
    id: "students",
    labelKey: "nav.students",
    href: "/students",
    icon: Users,
    roles: ["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER", "PARENT"],
  },
  {
    id: "parent-students",
    labelKey: "nav.parentStudents",
    href: "/parent-students",
    icon: Users,
    roles: ["SUPER_ADMIN", "MOSQUE_ADMIN"],
  },
  {
    id: "circles",
    labelKey: "nav.circles",
    href: "/circles",
    icon: CircleDot,
    roles: ["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER", "STUDENT"],
  },
  {
    id: "enrollments",
    labelKey: "nav.enrollments",
    href: "/enrollments",
    icon: ClipboardList,
    roles: ["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER"],
  },
  {
    id: "attendance",
    labelKey: "nav.attendance",
    href: "/attendance",
    icon: UserCheck,
    roles: ["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER"],
  },
  {
    id: "achievements",
    labelKey: "nav.achievements",
    href: "/achievements",
    icon: Award,
    roles: ["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER", "STUDENT"],
  },
  {
    id: "messages",
    labelKey: "nav.messages",
    href: "/messages",
    icon: MessageSquare,
    roles: ["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER", "STUDENT", "PARENT"],
  },
  {
    id: "notifications",
    labelKey: "nav.notifications",
    href: "/notifications",
    icon: Bell,
    roles: ["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER", "STUDENT", "PARENT"],
  },
  {
    id: "payments",
    labelKey: "nav.payments",
    href: "/payments",
    icon: CreditCard,
    roles: ["SUPER_ADMIN", "MOSQUE_ADMIN"],
  },
  {
    id: "reports",
    labelKey: "nav.reports",
    href: "/reports",
    icon: FileText,
    roles: ["SUPER_ADMIN", "MOSQUE_ADMIN"],
  },
  {
    id: "profile",
    labelKey: "nav.profile",
    href: "/settings/profile",
    icon: Settings,
    roles: ["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER", "STUDENT", "PARENT"],
  },
  {
    id: "progress",
    labelKey: "nav.progress",
    href: "/dashboard",
    icon: BookOpen,
    roles: ["STUDENT"],
  },
];

export function getNavItemsForRole(role: string): NavItem[] {
  const normalized = role.toUpperCase().replace(/-/g, "_") as UserRole;
  return APP_NAV_ITEMS.filter((item) => item.roles.includes(normalized));
}

export function getDefaultLandingPath(role: string): string {
  const normalized = role.toUpperCase().replace(/-/g, "_") as UserRole;
  switch (normalized) {
    case "SUPER_ADMIN":
      return "/mosques";
    case "MOSQUE_ADMIN":
      return "/admin";
    case "TEACHER":
      return "/circles";
    case "STUDENT":
      return "/dashboard";
    case "PARENT":
      return "/students";
    default:
      return "/dashboard";
  }
}

export function normalizeApiRole(role: string): UserRole {
  return role.toUpperCase().replace(/-/g, "_") as UserRole;
}
