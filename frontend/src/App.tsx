import { RouterProvider } from "react-router-dom";
import { Toaster } from "sonner";

import { AuthProvider } from "./features/auth/context/auth-provider.tsx";
import { router } from "./routes/index.tsx";

export default function App() {
  return (
    <AuthProvider>
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
    </AuthProvider>
  );
}
