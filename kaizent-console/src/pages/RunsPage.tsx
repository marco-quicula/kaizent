import { Alert, Loader, Pagination, Stack, Table, Text, Title } from "@mantine/core";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate } from "react-router";
import { api } from "../api/client";
import { RunStatusBadge } from "../components/RunStatusBadge";
import { formatDateTime } from "../format";

/** How often the run list is refreshed. Spec 0010 replaces polling with live updates (SSE). */
export const RUNS_REFRESH_MS = 5000;

/** Runs per page. */
export const RUNS_PAGE_SIZE = 50;

/** The run queue, newest first. */
export function RunsPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const runs = useQuery({
    queryKey: ["runs", page],
    queryFn: async () => {
      const { data, error } = await api.GET("/runs", {
        params: { query: { page, size: RUNS_PAGE_SIZE } },
      });
      if (error || !data) {
        throw new Error("Could not load the runs.");
      }
      return data;
    },
    refetchInterval: RUNS_REFRESH_MS,
    placeholderData: keepPreviousData,
  });
  const pages = runs.data ? Math.ceil(runs.data.totalItems / RUNS_PAGE_SIZE) : 0;

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
      {pages > 1 && (
        <Pagination
          total={pages}
          value={page + 1}
          onChange={(value) => setPage(value - 1)}
          aria-label="Run pages"
        />
      )}
    </Stack>
  );
}
