import { QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider } from "react-router-dom";
import { Toaster } from "sonner";

import { AuthProvider } from "./features/auth/context/auth-provider.tsx";
import { WorkspaceProvider } from "./features/workspace/index.ts";
import { queryClient } from "./lib/query-client.ts";
import { router } from "./routes/index.tsx";
import { TooltipProvider } from "./components/ui/tooltip";

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <WorkspaceProvider>
          <TooltipProvider>
            <RouterProvider router={router} />
            <Toaster
              position="top-center"
              richColors
              closeButton
              toastOptions={{
                classNames: {
                  toast: "font-sans",
                },
              }}
            />
          </TooltipProvider>
        </WorkspaceProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}
