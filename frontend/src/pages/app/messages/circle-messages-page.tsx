import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeftIcon } from "lucide-react";
import { useEffect, useMemo } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header.tsx";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { useCircle } from "@/features/circles/hooks/use-circles.ts";
import {
  useCircleMessages,
  useCreateMessage,
} from "@/features/messages/hooks/use-messages.ts";
import {
  messageCreateSchema,
  type MessageCreateBody,
} from "@/features/messages/schemas/message.schema.ts";
import type { MessageResponse } from "@/features/messages/types/index.ts";
import { useTeacher } from "@/features/teachers/hooks/use-teachers.ts";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";
import { formatShortId } from "@/lib/format/ids.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { cn } from "@/lib/utils.ts";

function formatInstant(value: string): string {
  return new Date(value).toLocaleString();
}

export function CircleMessagesPage() {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const { locale, id: circleId } = useParams<{ locale: string; id: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const { params, setPage } = usePagination();
  const { data: circle } = useCircle(circleId ?? "", { enabled: !!circleId });
  const { data: teacher } = useTeacher(circle?.teacherId ?? "", {
    enabled: !!circle?.teacherId,
  });
  const { data, isLoading } = useCircleMessages(circleId, params);
  const createMessage = useCreateMessage();

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors },
  } = useForm<MessageCreateBody>({
    resolver: zodResolver(messageCreateSchema),
    defaultValues: {
      receiverId: "",
      circleId: circleId ?? "",
      content: "",
    },
  });

  const receiverId = watch("receiverId");

  const receiverOptions = useMemo(() => {
    const byName = new Map<string, string>();
    const note = (userId: string, name?: string) => {
      if (!userId || userId === user?.userId) return;
      if (!byName.has(userId)) {
        byName.set(userId, name ?? formatShortId(userId));
      }
    };
    if (teacher?.userId) note(teacher.userId, teacher.userName);
    for (const message of data?.content ?? []) {
      note(message.senderId, message.senderName);
      note(message.receiverId, message.receiverName);
    }
    return [...byName.entries()].map(([userId, label]) => ({ userId, label }));
  }, [data?.content, teacher, user?.userId]);

  useEffect(() => {
    if (circleId) setValue("circleId", circleId);
  }, [circleId, setValue]);

  useEffect(() => {
    if (!receiverId && receiverOptions.length > 0) {
      setValue("receiverId", receiverOptions[0]!.userId);
    }
  }, [receiverId, receiverOptions, setValue]);

  const threadMessages = useMemo(() => {
    const messages = [...(data?.content ?? [])];
    messages.sort(
      (a, b) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime(),
    );
    return messages;
  }, [data?.content]);

  async function onSubmit(values: MessageCreateBody) {
    if (!user || !circleId) return;

    try {
      await createMessage.mutateAsync({
        senderId: user.userId,
        receiverId: values.receiverId,
        circleId,
        content: values.content,
      });
      reset({ receiverId: values.receiverId, circleId, content: "" });
      toast.success(t("messages.sendSuccess"));
    } catch (error) {
      toast.error(toMutationError(error, t).message);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={circle?.name ?? t("messages.thread")}
        description={t("messages.threadDescription", {
          circle: circle?.name ?? t("messages.thread"),
        })}
        actions={
          <Button variant="outline" size="sm" asChild>
            <Link to={`/${localePrefix}/messages`}>
              <ArrowLeftIcon data-icon="inline-start" />
              {t("messages.backToInbox")}
            </Link>
          </Button>
        }
      />

      <Card className="flex min-h-[28rem] flex-col">
        <CardContent className="flex flex-1 flex-col gap-4 p-4">
          {isLoading ? (
            <div className="flex flex-col gap-2">
              {Array.from({ length: 4 }).map((_, index) => (
                <Skeleton key={index} className="h-16 w-full" />
              ))}
            </div>
          ) : threadMessages.length === 0 ? (
            <p className="py-8 text-center text-sm text-muted-foreground">
              {t("messages.emptyThread")}
            </p>
          ) : (
            <ScrollArea className="h-[24rem] pr-4">
              <div className="flex flex-col gap-3">
                {threadMessages.map((message) => (
                  <MessageBubble
                    key={message.id}
                    message={message}
                    isOwn={message.senderId === user?.userId}
                  />
                ))}
              </div>
            </ScrollArea>
          )}

          {data && data.totalPages > 1 ? (
            <div className="flex items-center justify-between gap-4 border-t border-border pt-4">
              <p className="text-sm text-muted-foreground">
                {t("table.pageInfo", {
                  page: data.pageNumber + 1,
                  total: data.totalPages,
                  count: data.totalElements,
                })}
              </p>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={data.pageNumber <= 0}
                  onClick={() => setPage(data.pageNumber - 1)}
                >
                  {t("table.previous")}
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={data.last}
                  onClick={() => setPage(data.pageNumber + 1)}
                >
                  {t("table.next")}
                </Button>
              </div>
            </div>
          ) : null}

          <form
            className="flex flex-col gap-4 border-t border-border pt-4"
            onSubmit={handleSubmit(onSubmit)}
          >
            <FieldGroup>
              <Field data-invalid={!!errors.receiverId}>
                <FieldLabel>{t("messages.receiver")}</FieldLabel>
                <Select
                  value={receiverId}
                  onValueChange={(value) => setValue("receiverId", value)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t("messages.receiverPlaceholder")} />
                  </SelectTrigger>
                  <SelectContent>
                    {receiverOptions.map((option) => (
                      <SelectItem key={option.userId} value={option.userId}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.receiverId ? (
                  <p className="text-sm text-destructive">
                    {errors.receiverId.message}
                  </p>
                ) : null}
              </Field>
              <Field data-invalid={!!errors.content}>
                <FieldLabel>{t("messages.content")}</FieldLabel>
                <Textarea rows={3} {...register("content")} />
                {errors.content ? (
                  <p className="text-sm text-destructive">
                    {errors.content.message}
                  </p>
                ) : null}
              </Field>
            </FieldGroup>
            <Button
              type="submit"
              disabled={createMessage.isPending || receiverOptions.length === 0}
            >
              {createMessage.isPending ? <Spinner data-icon="inline-start" /> : null}
              {t("messages.send")}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

function MessageBubble({
  message,
  isOwn,
}: {
  message: MessageResponse;
  isOwn: boolean;
}) {
  return (
    <div className={cn("flex", isOwn ? "justify-end" : "justify-start")}>
      <div
        className={cn(
          "max-w-[80%] rounded-lg border px-3 py-2 text-sm",
          isOwn
            ? "border-primary/20 bg-primary/10"
            : "border-border bg-muted/50",
        )}
      >
        <div className="mb-1 flex items-center gap-2">
          <Badge variant="outline">{message.status}</Badge>
          <span className="text-xs text-muted-foreground">
            {formatInstant(message.sentAt)}
          </span>
        </div>
        <p className="whitespace-pre-wrap">{message.content}</p>
      </div>
    </div>
  );
}
