import { Alert, Loader, Stack, Table, Text, Title } from "@mantine/core";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { api } from "../api/client";
import { RunStatusBadge } from "../components/RunStatusBadge";
import { formatDateTime } from "../format";

/** How often the run list is refreshed. Spec 0010 replaces polling with live updates (SSE). */
export const RUNS_REFRESH_MS = 5000;

/** The run queue, newest first. */
export function RunsPage() {
  const navigate = useNavigate();
  const runs = useQuery({
    queryKey: ["runs"],
    queryFn: async () => {
      const { data, error } = await api.GET("/runs", { params: { query: { size: 50 } } });
      if (error || !data) {
        throw new Error("Could not load the runs.");
      }
      return data;
    },
    refetchInterval: RUNS_REFRESH_MS,
  });

  return (
    <Stack>
      <Title order={2}>Runs</Title>
      {runs.isPending && <Loader aria-label="Loading" />}
      {runs.isError && (
        <Alert color="red" title="Error">
          {runs.error.message}
        </Alert>
      )}
      {runs.data && runs.data.items.length === 0 && (
        <Text c="dimmed">
          No runs yet. Drop a ticket file into the workspace&apos;s inbox/ folder.
        </Text>
      )}
      {runs.data && runs.data.items.length > 0 && (
        <Table highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Ticket</Table.Th>
              <Table.Th>Title</Table.Th>
              <Table.Th>Status</Table.Th>
              <Table.Th>Created</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {runs.data.items.map((run) => (
              <Table.Tr
                key={run.id}
                onClick={() => navigate(`/runs/${run.id}`)}
                style={{ cursor: "pointer" }}
              >
                <Table.Td>{run.ticketKey}</Table.Td>
                <Table.Td>{run.title}</Table.Td>
                <Table.Td>
                  <RunStatusBadge status={run.status} />
                </Table.Td>
                <Table.Td>{formatDateTime(run.createdAt)}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
    </Stack>
  );
}
