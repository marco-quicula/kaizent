import { MantineProvider } from "@mantine/core";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";
import { RouterProvider, type createBrowserRouter } from "react-router";
import { theme } from "./theme";

type Router = ReturnType<typeof createBrowserRouter>;

export function App({
  router,
  queryClient: client,
}: {
  router: Router;
  queryClient?: QueryClient;
}) {
  const [queryClient] = useState(() => client ?? new QueryClient());
  return (
    <MantineProvider theme={theme}>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </MantineProvider>
  );
}
