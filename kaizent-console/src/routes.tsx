import { Navigate, type RouteObject } from "react-router";
import { Shell } from "./layout/Shell";
import { PlaceholderPage } from "./pages/PlaceholderPage";
import { RunDetailPage } from "./pages/RunDetailPage";
import { RunsPage } from "./pages/RunsPage";
import { SystemPage } from "./pages/SystemPage";

/** Navigation entries of the console shell. Pages are filled in by later specs. */
export const navigation = [
  { path: "/runs", label: "Runs", spec: "0002" },
  { path: "/lessons", label: "Lessons", spec: "0011" },
  { path: "/subjects", label: "Subjects", spec: "0012" },
  { path: "/intake", label: "Intake", spec: "0010" },
  { path: "/system", label: "System", spec: "0001" },
] as const;

export const routes: RouteObject[] = [
  {
    path: "/",
    element: <Shell />,
    children: [
      { index: true, element: <Navigate to="/runs" replace /> },
      { path: "runs", element: <RunsPage /> },
      { path: "runs/:id", element: <RunDetailPage /> },
      { path: "lessons", element: <PlaceholderPage title="Lessons" spec="0011" /> },
      { path: "subjects", element: <PlaceholderPage title="Subjects" spec="0012" /> },
      { path: "intake", element: <PlaceholderPage title="Intake" spec="0010" /> },
      { path: "system", element: <SystemPage /> },
      { path: "*", element: <PlaceholderPage title="Page not found" /> },
    ],
  },
];
